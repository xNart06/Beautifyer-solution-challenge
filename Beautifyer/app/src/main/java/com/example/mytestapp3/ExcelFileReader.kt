package com.example.mytestapp3

import android.content.Context
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory

fun readExcelFile(context: Context, fileName: String): List<List<String>> {
    val inputStream = context.assets.open(fileName)

    val workbook: Workbook = WorkbookFactory.create(inputStream)
    val sheet: Sheet = workbook.getSheetAt(0)

    val data = mutableListOf<List<String>>()

    for (row in sheet) {
        val rowData = row.map { cell ->
            when (cell.cellType) {
                CellType.NUMERIC -> cell.numericCellValue.toString()
                CellType.STRING -> cell.stringCellValue
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                else -> ""
            }
        }
        data.add(rowData)
    }
    inputStream.close()

    return data
}