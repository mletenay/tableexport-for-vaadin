package com.vaadin.addon.tableexport.demo;

import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import org.apache.commons.io.FilenameUtils;

import com.vaadin.addon.tableexport.CsvExport;
import com.vaadin.addon.tableexport.DefaultGridHolder;
import com.vaadin.addon.tableexport.ExcelExport;
import com.vaadin.addon.tableexport.TableHolder;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.DateRenderer;
import com.vaadin.ui.renderers.NumberRenderer;

@Theme("tableexport-theme")
@Widgetset("com.vaadin.addon.tableexport.demo.TableExportWidgetset")
public class TableExportUI extends UI {

    private static final long serialVersionUID = -5436901535719211794L;
    
    @WebServlet(urlPatterns = "/*", name = "TableExportUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = TableExportUI.class, productionMode = false)
    public static class TableExportUIServlet extends VaadinServlet {
        private static final long serialVersionUID = 9113961084041090666L;
    }

    private DataProvider<PayCheck, ?> dataProvider;
    private SimpleDateFormat sdf = new SimpleDateFormat("mm/dd/yy");
    private DecimalFormat df = new DecimalFormat("#0.0000");

    // from: http://dev-answers.blogspot.com/2006/06/how-do-you-print-java-classpath.html
    public String getClasspathString() {
        StringBuffer classpath = new StringBuffer();
        ClassLoader applicationClassLoader = this.getClass().getClassLoader();
        if (applicationClassLoader == null) {
            applicationClassLoader = ClassLoader.getSystemClassLoader();
        }
        URL[] urls = ((URLClassLoader) applicationClassLoader).getURLs();
        for (int i = 0; i < urls.length; i++) {
            classpath.append(urls[i].getFile()).append("\r\n");
        }

        return classpath.toString();
    }

    @Override
    protected void init(final VaadinRequest request) {
        System.out.println(getClasspathString());
        getPage().setTitle("Table Export Test");

        // Create the table
        List<PayCheck> data = new ArrayList<>();
        try {
            final PayCheck p1 = new PayCheck("John Smith", sdf.parse("09/17/2011"), 1000.0, 2, true, "garbage1");
            final PayCheck p2 = new PayCheck("John Smith", sdf.parse("09/24/2011"), 1000.0, 1, true, "garbage2");
            final PayCheck p3 = new PayCheck("Jane Doe", sdf.parse("08/31/2011"), 750.0, 20, false, "garbage3");
            final PayCheck p4 = new PayCheck("Jane Doe", sdf.parse("09/07/2011"), 750.0, 10000, false, "garbage4");
            data.add(p1);
            data.add(p2);
            data.add(p3);
            data.add(p4);
        } catch (final ParseException pe) {
        }
        dataProvider = DataProvider.ofCollection(data);

        TabSheet componentChoice = new TabSheet();
        componentChoice.addTab(createGridAndOptions(), "Grid (V8)");
        setContent(componentChoice);
    }

    public HorizontalLayout createGridAndOptions() {

        final Grid<PayCheck> grid = new Grid<>();

        grid.setDataProvider(dataProvider);

        // this also sets the order of the columns
        grid.addColumn(PayCheck::getName).setId("name").setCaption("Name");
        grid.addColumn(PayCheck::getDate).setId("date").setCaption("Date").setRenderer(new DateRenderer(sdf));
        grid.addColumn(PayCheck::getAmount).setId("amount").setCaption("Amount Earned").setRenderer(new NumberRenderer(df));
        grid.addColumn(PayCheck::getWeeks).setId("weeks").setCaption("Weeks Worked").setRenderer(new NumberRenderer(df));
        grid.addColumn(p->.0825 * p.getAmount()).setId("taxes").setCaption("Taxes Paid").setRenderer(new NumberRenderer(df));
        grid.addColumn(PayCheck::isManager).setId("manager").setCaption("Is Manager?");
        grid.setColumnOrder("name", "date", "amount", "weeks", "taxes", "manager");

        // put the Grid in the TableHolder after the grid is fully baked
        final TableHolder<PayCheck> tableHolder = new DefaultGridHolder<>(grid);

        grid.setWidth("650px");
        TabSheet gridOptionsTab = new TabSheet();
        gridOptionsTab.setWidth("300px");

        // create the layout with the main export options
        final VerticalLayout mainOptions = new VerticalLayout();
        mainOptions.setSpacing(true);
        //mainOptions.setWidth("400px");
        final Label headerLabel = new Label("Export Options");
        final Label verticalSpacer = new Label();
        verticalSpacer.setHeight("10px");
        final Label endSpacer = new Label();
        endSpacer.setHeight("10px");
        final TextField reportTitleField = new TextField("Report Title", "Demo Report");
        final TextField sheetNameField = new TextField("Sheet Name", "Grid Export");
        final TextField exportFileNameField = new TextField("Export Filename", "Grid-Export.xls");
        final TextField excelNumberFormat = new TextField("Excel Double Format", "#0.00");
        final TextField excelDateFormat = new TextField("Excel Date Format", "mm/dd/yyyy");
        final CheckBox totalsRowField = new CheckBox("Add Totals Row", true);
        final CheckBox rowHeadersField = new CheckBox("Treat first Column as Row Headers", true);
        final CheckBox exportAsCsvUsingXLS2CSVmra = new CheckBox("Export As CSV", false);
        exportAsCsvUsingXLS2CSVmra.addValueChangeListener(event -> {
                final String fn = exportFileNameField.getValue().toString();
                final String justName = FilenameUtils.getBaseName(fn);
                if ((Boolean) exportAsCsvUsingXLS2CSVmra.getValue()) {
                    exportFileNameField.setValue(justName + ".csv");
                } else {
                    exportFileNameField.setValue(justName + ".xls");
                }
                exportFileNameField.markAsDirty();
        });

        mainOptions.addComponent(headerLabel);
        mainOptions.addComponent(verticalSpacer);
        mainOptions.addComponent(reportTitleField);
        mainOptions.addComponent(sheetNameField);
        mainOptions.addComponent(exportFileNameField);
        mainOptions.addComponent(excelNumberFormat);
        mainOptions.addComponent(excelDateFormat);
        mainOptions.addComponent(totalsRowField);
        mainOptions.addComponent(rowHeadersField);
        mainOptions.addComponent(exportAsCsvUsingXLS2CSVmra);

        // create the export buttons
        final Resource export = FontAwesome.FILE_EXCEL_O;
        final Button regularExportButton = new Button("Regular Export");
        regularExportButton.setIcon(export);

        final Button overriddenExportButton = new Button("Enhanced Export");
        overriddenExportButton.setIcon(export);

        final Button twoTabsExportButton = new Button("Two Tab Test");
        twoTabsExportButton.setIcon(export);

        final Button SXSSFWorkbookExportButton = new Button("Export Using SXSSFWorkbook");
        SXSSFWorkbookExportButton.setIcon(export);

        final Button fontExampleExportButton = new Button("Andreas Font Test");
        fontExampleExportButton.setIcon(export);

        final Button noHeaderTestButton = new Button("Andreas No Header Test");
        noHeaderTestButton.setIcon(export);

            ExcelExport excelExport;
                if (!"".equals(sheetNameField.getValue().toString())) {
                    if ((Boolean) exportAsCsvUsingXLS2CSVmra.getValue()) {
                        excelExport = new CsvExport(tableHolder, sheetNameField.getValue().toString());
                    } else {
                        excelExport = new ExcelExport(tableHolder, sheetNameField.getValue().toString());
                    }
                } else {
                    if ((Boolean) exportAsCsvUsingXLS2CSVmra.getValue()) {
                        excelExport = new CsvExport(tableHolder);
                    } else {
                        excelExport = new ExcelExport(tableHolder);
                    }
                }
                if (!"".equals(reportTitleField.getValue().toString())) {
                    excelExport.setReportTitle(reportTitleField.getValue().toString());
                }
                if (!"".equals(exportFileNameField.getValue().toString())) {
                    excelExport.setExportFileName(exportFileNameField.getValue().toString());
                }
                excelExport.setDisplayTotals(((Boolean) totalsRowField.getValue()).booleanValue());
                excelExport.setRowHeaders(((Boolean) rowHeadersField.getValue()).booleanValue());
                excelExport.setExcelFormatOfColumn("date", excelDateFormat.getValue().toString());
                excelExport.setDoubleDataFormat(excelNumberFormat.getValue().toString());
        new FileDownloader(excelExport.getDownloadResource()).extend(regularExportButton);
                
                if (!"".equals(sheetNameField.getValue().toString())) {
                    if ((Boolean) exportAsCsvUsingXLS2CSVmra.getValue()) {
                        excelExport = new CsvExport(tableHolder, sheetNameField.getValue().toString());
                    } else {
                        excelExport = new EnhancedFormatExcelExport(tableHolder, sheetNameField.getValue().toString());
                    }
                } else {
                    if ((Boolean) exportAsCsvUsingXLS2CSVmra.getValue()) {
                        excelExport = new CsvExport(tableHolder);
                    } else {
                        excelExport = new EnhancedFormatExcelExport(tableHolder);
                    }
                }
                if (!"".equals(reportTitleField.getValue().toString())) {
                    excelExport.setReportTitle(reportTitleField.getValue().toString());
                }
                if (!"".equals(exportFileNameField.getValue().toString())) {
                    excelExport.setExportFileName(exportFileNameField.getValue().toString());
                }
                excelExport.setDisplayTotals(((Boolean) totalsRowField.getValue()).booleanValue());
                excelExport.setRowHeaders(((Boolean) rowHeadersField.getValue()).booleanValue());
                excelExport.setExcelFormatOfColumn("date", excelDateFormat.getValue().toString());
                excelExport.setDoubleDataFormat(excelNumberFormat.getValue().toString());
        new FileDownloader(excelExport.getDownloadResource()).extend(overriddenExportButton);

                excelExport = new ExcelExport(tableHolder, sheetNameField.getValue().toString(),
                        reportTitleField.getValue().toString(), exportFileNameField.getValue().toString(),
                        ((Boolean) totalsRowField.getValue()).booleanValue());
                if (!"".equals(exportFileNameField.getValue().toString())) {
                    excelExport.setExportFileName(exportFileNameField.getValue().toString());
                }
                excelExport.setRowHeaders(((Boolean) rowHeadersField.getValue()).booleanValue());
                excelExport.setExcelFormatOfColumn("date", excelDateFormat.getValue().toString());
                excelExport.setDoubleDataFormat(excelNumberFormat.getValue().toString());
                excelExport.convertTable();
                excelExport.setNextTableHolder(tableHolder, "Second Sheet");
        new FileDownloader(excelExport.getDownloadResource()).extend(twoTabsExportButton);

                excelExport = new FontExampleExcelExport(tableHolder, sheetNameField.getValue().toString());
                if (!"".equals(reportTitleField.getValue().toString())) {
                    excelExport.setReportTitle(reportTitleField.getValue().toString());
                }
                if (!"".equals(exportFileNameField.getValue().toString())) {
                    excelExport.setExportFileName(exportFileNameField.getValue().toString());
                }
                excelExport.setDisplayTotals(((Boolean) totalsRowField.getValue()).booleanValue());
                excelExport.setRowHeaders(((Boolean) rowHeadersField.getValue()).booleanValue());
                excelExport.setExcelFormatOfColumn("date", excelDateFormat.getValue().toString());
                excelExport.setDoubleDataFormat(excelNumberFormat.getValue().toString());
        new FileDownloader(excelExport.getDownloadResource()).extend(fontExampleExportButton);
                final SimpleDateFormat expFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
                excelExport = new ExcelExport(tableHolder, "Tätigkeiten");
                excelExport.excludeCollapsedColumns();
                excelExport.setDisplayTotals(true);
                excelExport.setRowHeaders(false);
                // removed umlaut from file name due to Vaadin 7 bug that caused file not to get
                // written
                excelExport.setExportFileName("Tatigkeiten-" + expFormat.format(new Date()) + ".xls");
        new FileDownloader(excelExport.getDownloadResource()).extend(noHeaderTestButton);
        mainOptions.addComponent(regularExportButton);
        mainOptions.addComponent(overriddenExportButton);
        mainOptions.addComponent(twoTabsExportButton);
        mainOptions.addComponent(fontExampleExportButton);
        mainOptions.addComponent(noHeaderTestButton);
        mainOptions.addComponent(endSpacer);

        gridOptionsTab.addTab(mainOptions, "Main");

        // add to window
        final HorizontalLayout gridAndOptions = new HorizontalLayout();
        gridAndOptions.setSpacing(true);
        gridAndOptions.setMargin(true);
        gridAndOptions.addComponent(grid);
        final Label horizontalSpacer = new Label();
        horizontalSpacer.setWidth("5px");
        gridAndOptions.addComponent(horizontalSpacer);
        gridAndOptions.addComponent(gridOptionsTab);

        return gridAndOptions;
    }
    
    public class PayCheck implements Serializable {
        private static final long serialVersionUID = 9064899449347530333L;
        private String name;
        private Date date;
        private double amount;
        private int weeks;
        private boolean manager;
        private Object garbage;

        public PayCheck(final String name, final Date date, final double amount, final int weeks,
                        final boolean manager, final Object garbageToIgnore) {
            super();
            this.name = name;
            this.date = date;
            this.amount = amount;
            this.weeks = weeks;
            this.manager = manager;
            this.garbage = garbageToIgnore;
        }

        public String getName() {
            return this.name;
        }

        public Date getDate() {
            return this.date;
        }

        public double getAmount() {
            return this.amount;
        }

        public int getWeeks() {
            return this.weeks;
        }

        public boolean isManager() {
            return this.manager;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public void setDate(final Date date) {
            this.date = date;
        }

        public void setAmount(final double amount) {
            this.amount = amount;
        }

        public void setWeeks(final int weeks) {
            this.weeks = weeks;
        }

        public void setManager(final boolean manager) {
            this.manager = manager;
        }

        public Object getGarbage() {
            return this.garbage;
        }

        public void setGarbage(final Object garbage) {
            this.garbage = garbage;
        }

    }
}
