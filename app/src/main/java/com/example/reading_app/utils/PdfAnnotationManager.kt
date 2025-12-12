package com.example.reading_app.utils

import android.content.Context
import com.example.reading_app.model.AnnotationPoint
import com.example.reading_app.model.AnnotationPosition
import com.example.reading_app.model.DrawingPath
import com.example.reading_app.model.PdfAnnotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object PdfAnnotationManager {
    private const val ANNOTATIONS_FILE = "pdf_annotations.json"
    
    /**
     * Save a PDF annotation
     */
    suspend fun saveAnnotation(context: Context, annotation: PdfAnnotation) = withContext(Dispatchers.IO) {
        try {
            val annotations = loadAnnotations(context).toMutableList()
            
            // Remove existing annotation with same ID (for updates)
            annotations.removeAll { it.id == annotation.id }
            
            // Add new/updated annotation
            annotations.add(annotation)
            
            // Save to file
            val annotationsFile = File(context.filesDir, ANNOTATIONS_FILE)
            val jsonArray = JSONArray()
            
            annotations.forEach { ann ->
                val jsonObject = JSONObject().apply {
                    put("id", ann.id)
                    put("bookFilePath", ann.bookFilePath)
                    put("pageNumber", ann.pageNumber)
                    put("type", ann.type.name)
                    put("note", ann.note ?: "")
                    put("color", ann.color)
                    put("timestamp", ann.timestamp)
                    
                    // Serialize drawing paths
                    if (ann.drawingPaths.isNotEmpty()) {
                        val pathsArray = JSONArray()
                        ann.drawingPaths.forEach { path ->
                            val pathObj = JSONObject().apply {
                                put("color", path.color)
                                put("strokeWidth", path.strokeWidth.toDouble())
                                
                                val pointsArray = JSONArray()
                                path.points.forEach { point ->
                                    pointsArray.put(JSONObject().apply {
                                        put("x", point.x.toDouble())
                                        put("y", point.y.toDouble())
                                    })
                                }
                                put("points", pointsArray)
                            }
                            pathsArray.put(pathObj)
                        }
                        put("drawingPaths", pathsArray)
                    }
                    
                    // Serialize position
                    ann.position?.let { pos ->
                        put("position", JSONObject().apply {
                            put("x", pos.x.toDouble())
                            put("y", pos.y.toDouble())
                        })
                    }
                    
                    ann.width?.let { put("width", it.toDouble()) }
                    ann.height?.let { put("height", it.toDouble()) }
                }
                jsonArray.put(jsonObject)
            }
            
            annotationsFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Load all PDF annotations from storage
     */
    suspend fun loadAnnotations(context: Context): List<PdfAnnotation> = withContext(Dispatchers.IO) {
        try {
            val annotationsFile = File(context.filesDir, ANNOTATIONS_FILE)
            
            if (!annotationsFile.exists()) {
                return@withContext emptyList()
            }
            
            val jsonArray = JSONArray(annotationsFile.readText())
            val annotations = mutableListOf<PdfAnnotation>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                // Deserialize drawing paths
                val drawingPaths = mutableListOf<DrawingPath>()
                if (jsonObject.has("drawingPaths")) {
                    val pathsArray = jsonObject.getJSONArray("drawingPaths")
                    for (j in 0 until pathsArray.length()) {
                        val pathObj = pathsArray.getJSONObject(j)
                        val points = mutableListOf<AnnotationPoint>()
                        
                        val pointsArray = pathObj.getJSONArray("points")
                        for (k in 0 until pointsArray.length()) {
                            val pointObj = pointsArray.getJSONObject(k)
                            points.add(AnnotationPoint(
                                x = pointObj.getDouble("x").toFloat(),
                                y = pointObj.getDouble("y").toFloat()
                            ))
                        }
                        
                        drawingPaths.add(DrawingPath(
                            points = points,
                            color = pathObj.getString("color"),
                            strokeWidth = pathObj.getDouble("strokeWidth").toFloat()
                        ))
                    }
                }
                
                // Deserialize position
                val position = if (jsonObject.has("position")) {
                    val posObj = jsonObject.getJSONObject("position")
                    AnnotationPosition(
                        x = posObj.getDouble("x").toFloat(),
                        y = posObj.getDouble("y").toFloat()
                    )
                } else null
                
                val annotation = PdfAnnotation(
                    id = jsonObject.getString("id"),
                    bookFilePath = jsonObject.getString("bookFilePath"),
                    pageNumber = jsonObject.getInt("pageNumber"),
                    type = com.example.reading_app.model.AnnotationType.valueOf(jsonObject.getString("type")),
                    note = jsonObject.optString("note").takeIf { it.isNotEmpty() },
                    color = jsonObject.getString("color"),
                    timestamp = jsonObject.getLong("timestamp"),
                    drawingPaths = drawingPaths,
                    position = position,
                    width = if (jsonObject.has("width")) jsonObject.getDouble("width").toFloat() else null,
                    height = if (jsonObject.has("height")) jsonObject.getDouble("height").toFloat() else null
                )
                annotations.add(annotation)
            }
            
            return@withContext annotations
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
    
    /**
     * Get annotations for a specific page of a book
     */
    suspend fun getAnnotationsForPage(
        context: Context, 
        bookFilePath: String, 
        pageNumber: Int
    ): List<PdfAnnotation> {
        return loadAnnotations(context).filter { 
            it.bookFilePath == bookFilePath && it.pageNumber == pageNumber 
        }.sortedBy { it.timestamp }
    }
    
    /**
     * Get all annotations for a specific book
     */
    suspend fun getAnnotationsForBook(context: Context, bookFilePath: String): List<PdfAnnotation> {
        return loadAnnotations(context).filter { it.bookFilePath == bookFilePath }
            .sortedWith(compareBy({ it.pageNumber }, { it.timestamp }))
    }
    
    /**
     * Delete a specific annotation
     */
    suspend fun deleteAnnotation(context: Context, annotationId: String) = withContext(Dispatchers.IO) {
        try {
            val annotations = loadAnnotations(context).toMutableList()
            annotations.removeAll { it.id == annotationId }
            
            val annotationsFile = File(context.filesDir, ANNOTATIONS_FILE)
            val jsonArray = JSONArray()
            
            annotations.forEach { ann ->
                val jsonObject = JSONObject().apply {
                    put("id", ann.id)
                    put("bookFilePath", ann.bookFilePath)
                    put("pageNumber", ann.pageNumber)
                    put("type", ann.type.name)
                    put("note", ann.note ?: "")
                    put("color", ann.color)
                    put("timestamp", ann.timestamp)
                    
                    if (ann.drawingPaths.isNotEmpty()) {
                        val pathsArray = JSONArray()
                        ann.drawingPaths.forEach { path ->
                            val pathObj = JSONObject().apply {
                                put("color", path.color)
                                put("strokeWidth", path.strokeWidth.toDouble())
                                
                                val pointsArray = JSONArray()
                                path.points.forEach { point ->
                                    pointsArray.put(JSONObject().apply {
                                        put("x", point.x.toDouble())
                                        put("y", point.y.toDouble())
                                    })
                                }
                                put("points", pointsArray)
                            }
                            pathsArray.put(pathObj)
                        }
                        put("drawingPaths", pathsArray)
                    }
                    
                    ann.position?.let { pos ->
                        put("position", JSONObject().apply {
                            put("x", pos.x.toDouble())
                            put("y", pos.y.toDouble())
                        })
                    }
                    
                    ann.width?.let { put("width", it.toDouble()) }
                    ann.height?.let { put("height", it.toDouble()) }
                }
                jsonArray.put(jsonObject)
            }
            
            annotationsFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Update annotation note
     */
    suspend fun updateAnnotationNote(context: Context, annotationId: String, newNote: String) {
        val annotations = loadAnnotations(context)
        val annotation = annotations.find { it.id == annotationId }
        if (annotation != null) {
            val updated = annotation.copy(note = newNote.takeIf { it.isNotEmpty() })
            saveAnnotation(context, updated)
        }
    }
}
