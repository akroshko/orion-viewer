/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2013  Michael Bogdanov & Co
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package universe.constellation.orion.viewer.djvu

import android.graphics.RectF
import universe.constellation.orion.viewer.Bitmap
import universe.constellation.orion.viewer.PageDimension
import universe.constellation.orion.viewer.document.AbstractDocument
import universe.constellation.orion.viewer.document.OutlineItem
import universe.constellation.orion.viewer.document.PageWithAutoCrop
import universe.constellation.orion.viewer.pdf.DocInfo
import universe.constellation.orion.viewer.timing
import java.util.Locale

class DjvuDocument(filePath: String) : AbstractDocument(filePath) {

    inner class DjvuPage(pageNum: Int) : PageWithAutoCrop(pageNum) {
        @Volatile
        private var pagePointer: Long = 0
        @Volatile
        private var destroyed = false

        override fun getPageDimension(): PageDimension {
            //TODO default page size
            if (docPointer == 0L || destroyed) return dimensionForCorruptedPage()

            PageDimension().also {
                timing("Page $pageNum info calculation") {
                    return getPageDimension(docPointer, pageNum, it) ?: dimensionForCorruptedPage()
                }
            }
        }

        override fun readPageDataForRendering() {
            if (destroyed) return
            if (pagePointer == 0L && docPointer != 0L) {
                pagePointer = gotoPageInternal(docPointer, pageNum)
            }
        }

        override fun renderPage(
            bitmap: Bitmap,
            zoom: Double,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            leftOffset: Int,
            topOffset: Int
        ) {
            if (destroyed) return
            readPageDataForRendering()
            renderPage(
                pageNum,
                pagePointer,
                bitmap,
                zoom,
                left,
                top,
                right,
                bottom,
                leftOffset,
                topOffset
            )
        }

        override fun destroy() {
            if (!destroyed) {
                destroyed = true
                if (pagePointer != 0L) {
                    releasePage(pagePointer)
                    pagePointer = 0
                }
                destroyPage(this)
            }
        }
    }

    @Volatile
    private var docPointer = 0L
    private var contextPointer = 0L
    private val docInfo = DocInfo()

    override val outline: Array<OutlineItem>?
        get() = getOutline(docPointer)

    init {
        contextPointer = initContext()
        docInfo.fileName = filePath
        if (contextPointer == 0L) throw RuntimeException("Can't create djvu contextPointer").also { destroy() }
        docPointer = openFile(filePath, docInfo, contextPointer)
        if (docPointer == 0L) throw RuntimeException("Can't open file $filePath").also { destroy() }
    }

    override val pageCount: Int
        get() = docInfo.pageCount

    override fun createPage(pageNum: Int): PageWithAutoCrop {
        return DjvuPage(pageNum)
    }

    @Synchronized
    private fun renderPage(pageNum: Int, pagePointer: Long, bitmap: Bitmap, zoom: Double, left: Int, top: Int, right: Int, bottom: Int, leftOffset: Int, topOffset: Int) {
        //destroyed, can be called in non-ui thread
        if (docPointer == 0L || pagePointer == 0L) return

        timing("Page $pageNum ($pagePointer) rendering") {
            drawPage(docPointer, pagePointer, bitmap, zoom.toFloat(), bitmap.width, bitmap.height, leftOffset + left, topOffset + top, right - left, bottom - top, left, top)
        }
    }

    @Synchronized
    override fun destroy() {
        destroyPages()
        destroying(docPointer, contextPointer)
        docPointer = 0
        contextPointer = 0
    }

    override val title: String?
        get() = null

    external override fun setContrast(contrast: Int)
    external override fun setThreshold(threshold: Int)
    external fun getOutline(doc: Long): Array<OutlineItem>
    external fun getText(doc: Long, pageNumber: Int, absoluteX: Int, absoluteY: Int, width: Int, height: Int): String
    external fun releasePage(page: Long)

    override fun needPassword() = false

    override fun authenticate(password: String) = true

    override fun searchPage(pageNumber: Int, text: String): Array<RectF>? {
        val textToSearch = text.lowercase(Locale.getDefault())

        val strings = ArrayList<String>(500)
        val positions = ArrayList<RectF>(500)
        getPageText(docPointer, pageNumber, strings, positions)

        var prevIndex = 0
        val indexes = ArrayList<Int>(500)
        val builder = StringBuilder()
        for (i in positions.indices) {
            val string = strings[i]
            builder.append(string.lowercase(Locale.getDefault()))
            val length = builder.length
            for (j in prevIndex until length) {
                indexes.add(i)
            }
            prevIndex = length
        }

        val searchFrom = 0
        val result = ArrayList<RectF>()
        val textLength = textToSearch.length
        var i = builder.indexOf(textToSearch, searchFrom)
        while (i != -1) {
            val start = indexes[i]
            val end = indexes[i + textLength - 1]

            val rectF = RectF(getSafeRectInPosition(positions, start))
            rectF.union(getSafeRectInPosition(positions, end))
            result.add(rectF)
            i += textLength
            i = builder.indexOf(textToSearch, i)
        }

        return result.toTypedArray()
    }

    private fun getSafeRectInPosition(rects: List<RectF>, position: Int): RectF {
        //TODO
        return rects[position]
    }

    override fun getText(pageNum: Int, absoluteX: Int, absoluteY: Int, width: Int, height: Int, singleWord: Boolean) =
            getText(docPointer, pageNum, absoluteX, absoluteY, width, height)

    companion object {

        init {
            System.loadLibrary("djvu")
        }

        @JvmStatic
        external fun initContext(): Long

        @JvmStatic
        external fun openFile(filename: String, info: DocInfo, context: Long): Long

        @JvmStatic
        external fun gotoPageInternal(doc: Long, pageNum: Int): Long

        @JvmStatic
        external fun getPageDimension(doc: Long, pageNum: Int, info: PageDimension): PageDimension?

        @JvmStatic
        external fun drawPage(doc: Long, page: Long, bitmap: Bitmap, zoom: Float, bitmapWidth: Int, bitmapHeight: Int,
                              patchX: Int, patchY: Int,
                              patchW: Int, patchH: Int,
                              originX: Int, originY: Int): Boolean

        @JvmStatic
        external fun destroying(doc: Long, context: Long)

        @JvmStatic
        external fun getPageText(doc: Long, pageNumber: Int, stringBuilder: ArrayList<*>, positions: ArrayList<*>): Boolean
    }
}
