package com.example.mytestapp3

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.FileInputStream


fun main() {
    val projectDirectory = System.getProperty("user.dir")
    val filePath = "$projectDirectory\\app\\src\\main\\assets\\Dataset.xlsx"
    readExcelFile(filePath)
}

fun readExcelFile(filePath: String) {
    val file = FileInputStream(File(filePath))
    val workbook: Workbook = WorkbookFactory.create(file)
    val sheet: Sheet = workbook.getSheetAt(0)

    val columnHeaders = sheet.getRow(0).map { it.toString() }

    for (row in sheet) {
        for (cell in row) {
            val cellValue = when (cell.cellType) {
                CellType.NUMERIC -> cell.numericCellValue.toString()
                CellType.STRING -> cell.stringCellValue
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                else -> ""
            }
            println("${columnHeaders[cell.columnIndex]}: $cellValue")
        }
        println("--------------")
    }
    file.close()
}



/*
fun readExcelFile(filePath: String) {
    val file = FileInputStream(File(filePath))
    val workbook: Workbook = WorkbookFactory.create(file)
    val sheet: Sheet = workbook.getSheetAt(0)
    for (row in sheet) {
        for (cell in row) {
            val cellValue = when (cell.cellType) {
                CellType.NUMERIC -> cell.numericCellValue.toString()
                CellType.STRING -> cell.stringCellValue
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                else -> ""
            }
            println(cellValue)
        }
    }
    file.close()
}
*/
