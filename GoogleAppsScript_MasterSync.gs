/**
 * NepSecure Portfolio Tracker - User Spreadsheet Apps Script
 * Public Master Sheet ID: 1dNE3DhX2d0DGcP8GFpe2bSwHAXZvOt7l4C6QSPVffbc
 *
 * Setup Instructions:
 * 1. Open your personal portfolio Google Sheet.
 * 2. Click Extensions > Apps Script.
 * 3. Replace all existing code in Code.gs with this script.
 * 4. Save and reload your spreadsheet to see the custom menu:
 *    "NepSecure Tracker"
 *    ├── "Sync Master Data & Refresh Formulas"
 *    └── "Setup Default Formats & Headers"
 */

var MASTER_SHEET_ID = "1dNE3DhX2d0DGcP8GFpe2bSwHAXZvOt7l4C6QSPVffbc";

/**
 * Creates custom menu on Spreadsheet Open
 */
function onOpen() {
  const ui = SpreadsheetApp.getUi();
  ui.createMenu('NepSecure Tracker')
    .addItem('Sync Master Data & Refresh Formulas', 'syncMasterDataAndFormulas')
    .addItem('Setup Default Formats & Headers', 'setupHeadersAndFormulas')
    .addToUi();
}

/**
 * Main Sync Function: Copies 'stocks' & 'market holiday' tabs from Master Sheet
 * and applies auto-calculation formulas to 'Current' sheet.
 */
function syncMasterDataAndFormulas() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  
  try {
    const masterSs = SpreadsheetApp.openById(MASTER_SHEET_ID);
    
    // 1. Sync 'stocks' tab
    const masterStocksSheet = masterSs.getSheetByName("stocks");
    if (masterStocksSheet) {
      let targetStocksSheet = ss.getSheetByName("stocks");
      if (!targetStocksSheet) {
        targetStocksSheet = ss.insertSheet("stocks");
      }
      const data = masterStocksSheet.getDataRange().getValues();
      targetStocksSheet.clearContents();
      if (data.length > 0) {
        targetStocksSheet.getRange(1, 1, data.length, data[0].length).setValues(data);
      }
    }
    
    // 2. Sync 'market holiday' tab
    const masterHolidaySheet = masterSs.getSheetByName("market holiday");
    if (masterHolidaySheet) {
      let targetHolidaySheet = ss.getSheetByName("market holiday");
      if (!targetHolidaySheet) {
        targetHolidaySheet = ss.insertSheet("market holiday");
      }
      const data = masterHolidaySheet.getDataRange().getValues();
      targetHolidaySheet.clearContents();
      if (data.length > 0) {
        targetHolidaySheet.getRange(1, 1, data.length, data[0].length).setValues(data);
      }
    }

    // 3. Setup Formulas in Current sheet
    setupHeadersAndFormulas();

    SpreadsheetApp.getUi().alert("✅ Success: Master market data synced ('stocks' & 'market holiday') and formulas updated!");
  } catch (error) {
    Logger.log("Error syncing master sheet: " + error);
    SpreadsheetApp.getUi().alert("⚠️ Sync Notice: Using IMPORTRANGE formulas fallback. Details: " + error.message);
    setupImportRangeFallback();
  }
}

/**
 * Applies headers and formulas on 'Current' sheet (Rows 9 onwards)
 */
function setupHeadersAndFormulas() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let currentSheet = ss.getSheetByName("Current");
  if (!currentSheet) {
    currentSheet = ss.insertSheet("Current");
  }

  // Set Row 8 Headers
  const headers = [
    "Symbol",                    // Col A
    "Quantity",                  // Col B
    "Total Investment",          // Col C
    "Sales/Dividend Earned",     // Col D
    "Current Investment",        // Col E
    "WACC",                      // Col F
    "Current Price",             // Col G
    "Market Value",              // Col H
    "Current Profit/Loss",       // Col I
    "Profit/Loss %",             // Col J
    "Sector"                     // Col K
  ];
  
  currentSheet.getRange(8, 1, 1, headers.length).setValues([headers]);
  currentSheet.getRange(8, 1, 1, headers.length)
    .setFontWeight("bold")
    .setBackground("#1E293B")
    .setFontColor("#FFFFFF");

  // Apply Formulas to Rows 9 to 100
  const maxRow = 100;
  for (let r = 9; r <= maxRow; r++) {
    // Current Investment: =IF(B9 > 0, IF(C9 > D9, MINUS(C9,D9), 0), 0)
    currentSheet.getRange(r, 5).setFormula(`=IF(B${r}>0, IF(C${r}>D${r}, MINUS(C${r},D${r}), 0), 0)`);
    
    // Current Price: =IF(ISBLANK(A9), "", IFERROR(VLOOKUP(A9, stocks!A:B, 2, FALSE), 0))
    currentSheet.getRange(r, 7).setFormula(`=IF(ISBLANK(A${r}), "", IFERROR(VLOOKUP(A${r}, stocks!A:B, 2, FALSE), 0))`);
    
    // Market Value: =IF(ISBLANK(A9), "", B9*G9)
    currentSheet.getRange(r, 8).setFormula(`=IF(ISBLANK(A${r}), "", B${r}*G${r})`);
    
    // Current Profit/Loss: =IF(ISBLANK(A9), "", H9-E9)
    currentSheet.getRange(r, 9).setFormula(`=IF(ISBLANK(A${r}), "", H${r}-E${r})`);
    
    // Profit/Loss %: =IF(E9>0, (H9-E9)/E9*100, 0)
    currentSheet.getRange(r, 10).setFormula(`=IF(E${r}>0, (H${r}-E${r})/E${r}*100, 0)`);
    
    // Sector: =IF(ISBLANK(A9), "", IFERROR(VLOOKUP(A9, stocks!A:E, 5, FALSE), "Others"))
    currentSheet.getRange(r, 11).setFormula(`=IF(ISBLANK(A${r}), "", IFERROR(VLOOKUP(A${r}, stocks!A:E, 5, FALSE), "Others"))`);
  }

  // Summary Totals in Row 5 for app compatibility (E5 = Total Market Value, O5 = Total Profit/Loss)
  currentSheet.getRange("E5").setFormula("=SUM(H9:H100)");
  currentSheet.getRange("O5").setFormula("=SUM(I9:I100)");
}

/**
 * IMPORTRANGE Fallback if direct script access requires authentication
 */
function setupImportRangeFallback() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  
  let stocksSheet = ss.getSheetByName("stocks") || ss.insertSheet("stocks");
  stocksSheet.getRange("A1").setFormula(`=IMPORTRANGE("${MASTER_SHEET_ID}", "stocks!A1:Z500")`);
  
  let holidaySheet = ss.getSheetByName("market holiday") || ss.insertSheet("market holiday");
  holidaySheet.getRange("A1").setFormula(`=IMPORTRANGE("${MASTER_SHEET_ID}", "market holiday!A1:Z100")`);
  
  setupHeadersAndFormulas();
}
