package com.example.reading_app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

object CoverExtractor {
    
    /**
     * Extract cover image from a PDF file
     * Returns the path to the saved cover image or null if extraction fails
     */
    fun extractPdfCover(context: Context, pdfFile: File): String? {
        return try {
            val fileDescriptor = ParcelFileDescriptor.open(
                pdfFile,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            
            val pdfRenderer = PdfRenderer(fileDescriptor)
            
            if (pdfRenderer.pageCount > 0) {
                val page = pdfRenderer.openPage(0) // First page
                
                // Create bitmap with appropriate size
                val bitmap = Bitmap.createBitmap(
                    page.width,
                    page.height,
                    Bitmap.Config.ARGB_8888
                )
                
                // Render page to bitmap
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                // Save bitmap to covers directory
                val coversDir = File(context.filesDir, "covers")
                if (!coversDir.exists()) {
                    coversDir.mkdirs()
                }
                
                val coverFile = File(coversDir, "${pdfFile.nameWithoutExtension}_cover.jpg")
                FileOutputStream(coverFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                
                // Clean up
                page.close()
                pdfRenderer.close()
                fileDescriptor.close()
                bitmap.recycle()
                
                coverFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Extract cover image from an EPUB file
     * Returns the path to the saved cover image or null if extraction fails
     */
    fun extractEpubCover(context: Context, epubFile: File): String? {
        return try {
            val zipFile = ZipFile(epubFile)
            var coverImagePath: String? = null
            
            // Try to find cover image by common patterns
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name.lowercase()
                
                // Look for common cover image names/paths
                if (!entry.isDirectory && 
                    (name.contains("cover") || name.contains("title")) &&
                    (name.endsWith(".jpg") || name.endsWith(".jpeg") || 
                     name.endsWith(".png") || name.endsWith(".gif"))) {
                    
                    // Extract this image
                    val coversDir = File(context.filesDir, "covers")
                    if (!coversDir.exists()) {
                        coversDir.mkdirs()
                    }
                    
                    val extension = name.substringAfterLast(".")
                    val coverFile = File(coversDir, "${epubFile.nameWithoutExtension}_cover.$extension")
                    
                    zipFile.getInputStream(entry).use { input ->
                        FileOutputStream(coverFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    coverImagePath = coverFile.absolutePath
                    break
                }
            }
            
            zipFile.close()
            
            // If no cover found by name, try to parse OPF file for cover reference
            if (coverImagePath == null) {
                coverImagePath = extractEpubCoverFromMetadata(context, epubFile)
            }
            
            coverImagePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Try to extract cover by parsing EPUB metadata (container.xml and OPF files)
     */
    private fun extractEpubCoverFromMetadata(context: Context, epubFile: File): String? {
        return try {
            val zipFile = ZipFile(epubFile)
            
            // First, find the OPF file location from container.xml
            val containerEntry = zipFile.getEntry("META-INF/container.xml")
            if (containerEntry != null) {
                val containerXml = zipFile.getInputStream(containerEntry).bufferedReader().readText()
                
                // Simple parsing to find OPF file path
                val opfPathRegex = """full-path="([^"]+\.opf)"""".toRegex()
                val opfPath = opfPathRegex.find(containerXml)?.groupValues?.get(1)
                
                if (opfPath != null) {
                    val opfEntry = zipFile.getEntry(opfPath)
                    if (opfEntry != null) {
                        val opfXml = zipFile.getInputStream(opfEntry).bufferedReader().readText()
                        
                        // Look for cover reference in metadata
                        val coverRefRegex = """<meta\s+name="cover"\s+content="([^"]+)"""".toRegex()
                        val coverId = coverRefRegex.find(opfXml)?.groupValues?.get(1)
                        
                        if (coverId != null) {
                            // Find the actual file with this ID in manifest
                            val manifestRegex = """<item\s+id="$coverId"[^>]*href="([^"]+)"""".toRegex()
                            val coverHref = manifestRegex.find(opfXml)?.groupValues?.get(1)
                            
                            if (coverHref != null) {
                                // Construct full path to cover image
                                val opfDir = opfPath.substringBeforeLast("/")
                                val fullCoverPath = if (opfDir.isNotEmpty()) {
                                    "$opfDir/$coverHref"
                                } else {
                                    coverHref
                                }
                                
                                val coverEntry = zipFile.getEntry(fullCoverPath)
                                if (coverEntry != null) {
                                    val coversDir = File(context.filesDir, "covers")
                                    if (!coversDir.exists()) {
                                        coversDir.mkdirs()
                                    }
                                    
                                    val extension = coverHref.substringAfterLast(".")
                                    val coverFile = File(coversDir, "${epubFile.nameWithoutExtension}_cover.$extension")
                                    
                                    zipFile.getInputStream(coverEntry).use { input ->
                                        FileOutputStream(coverFile).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    
                                    zipFile.close()
                                    return coverFile.absolutePath
                                }
                            }
                        }
                    }
                }
            }
            
            zipFile.close()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Delete cover image file
     */
    fun deleteCover(coverPath: String?) {
        if (coverPath != null) {
            try {
                File(coverPath).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
