/**
 * XLSX 导出工具函数。
 * 使用 SheetJS (xlsx) 库生成标准 Excel 文件，点击导出时再加载重依赖。
 */

/**
 * 将数据导出为 XLSX 文件。
 * @param {Object} options
 * @param {string[]} options.headers - 表头名称数组
 * @param {Array<Array<any>>} options.rows - 数据行数组，每行是值的数组
 * @param {string} options.filename - 文件名（不含扩展名）
 * @param {string} [options.sheetName='Sheet1'] - 工作表名称
 */
export async function exportToXlsx({ headers, rows, filename, sheetName = 'Sheet1' }) {
  // xlsx 体积较大，只在用户真正导出时加载，避免拖慢管理端首次路由加载。
  const XLSX = await import('xlsx')
  const worksheetData = [headers, ...rows]
  const worksheet = XLSX.utils.aoa_to_sheet(worksheetData)

  // 自动调整列宽
  const colWidths = headers.map((header, colIndex) => {
    const maxLen = Math.max(
      header.length,
      ...rows.map(row => String(row[colIndex] ?? '').length)
    )
    return { wch: Math.min(maxLen + 4, 50) }
  })
  worksheet['!cols'] = colWidths

  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, sheetName)
  XLSX.writeFile(workbook, `${filename}.xlsx`)
}
