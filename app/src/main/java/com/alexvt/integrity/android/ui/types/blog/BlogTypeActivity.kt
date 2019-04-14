/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.types.blog

import android.content.Context
import android.view.LayoutInflater
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.alexvt.integrity.R
import com.alexvt.integrity.lib.android.ui.types.DataTypeActivity
import com.alexvt.integrity.lib.core.util.LinkUtil
import com.alexvt.integrity.lib.core.util.WebArchiveFilesUtil
import com.alexvt.integrity.lib.android.util.AndroidWebPageLoader
import com.alexvt.integrity.lib.core.data.metadata.SnapshotMetadata
import com.alexvt.integrity.core.data.types.blog.BlogTypeMetadata
import com.alexvt.integrity.core.data.types.blog.IndexedPagination
import com.alexvt.integrity.core.data.types.blog.LinkedPagination
import com.jakewharton.rxbinding3.widget.checkedChanges
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class BlogTypeActivity : DataTypeActivity() {

    private val TAG = BlogTypeActivity::class.java.simpleName

    private lateinit var content : com.alexvt.integrity.databinding.BlogTypeContentBinding
    private lateinit var controls : com.alexvt.integrity.databinding.BlogTypeControlsBinding
    private lateinit var filter : com.alexvt.integrity.databinding.BlogTypeFilterBinding

    // Type implementation

    /**
     * Gets screen name of this data type.
     */
    override fun getTypeName() = "Blog"

    /**
     * Gets an instance of type specific metadata for this type.
     */
    override fun getTypeMetadataNewInstance() = BlogTypeMetadata()

    /**
     * Creates binding for this type content view.
     *
     * Side effects: reads layout ID, saves binding.
     */
    override fun inflateContentView(context: Context): ViewDataBinding {
        content = DataBindingUtil.inflate(LayoutInflater.from(context),
                R.layout.blog_type_content, null, false)
        return content
    }

    /**
     * Creates binding for this type controls view.
     *
     * Side effects: reads layout ID, saves binding.
     */
    override fun inflateControlsView(context: Context): ViewDataBinding {
        controls = DataBindingUtil.inflate(LayoutInflater.from(context),
                R.layout.blog_type_controls, null, false)
        return controls
    }

    /**
     * Creates binding for this type filters and options.
     *
     * Side effects: reads layout ID, saves binding.
     */
    override fun inflateFilterView(context: Context): ViewDataBinding {
        filter = DataBindingUtil.inflate(LayoutInflater.from(context),
                R.layout.blog_type_filter, null, false)
        return filter
    }

    /**
     * Sets options in bound view for the given snapshot.
     *
     * Side effects: modifies bound views.
     */
    override fun fillInTypeOptions(snapshot: SnapshotMetadata, isEditable: Boolean) {
        controls.etShortUrl.isEnabled = isEditable
        controls.etShortUrl.setText(LinkUtil.getShortFormUrl(getLatestSnapshotUrl(snapshot)))
        controls.etShortUrl.setOnEditorActionListener {
            _, _, _ -> goToWebPage(snapshot, controls.etShortUrl.text.toString())
        }
        controls.bGo.isEnabled = isEditable
        controls.bGo.setOnClickListener {
            _ -> goToWebPage(snapshot, controls.etShortUrl.text.toString())
        }

        filter.rvRelatedLinkList.adapter = RelatedLinkRecyclerAdapter(arrayListOf(), this)
        filter.rvOfflineLinkList.adapter = OfflineLinkRecyclerAdapter(arrayListOf(), this)

        filter.etRelatedLinkFilter.isEnabled = isEditable
        filter.etRelatedLinkFilter.append(getTypeMetadata(snapshot).relatedPageLinksFilter)
        filter.etRelatedLinkFilter.textChanges()
                .debounce(800, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateMatchedRelatedLinkList() }
        filter.etLinkPattern.isEnabled = isEditable
        filter.etLinkPattern.append(getTypeMetadata(snapshot).relatedPageLinksPattern)
        filter.etLinkPattern.textChanges()
                .debounce(800, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateMatchedRelatedLinkList() }

        filter.cbLoadImages.isEnabled = isEditable
        filter.cbLoadImages.isChecked = getTypeMetadata(snapshot).loadImages
        filter.cbDesktopSite.isEnabled = isEditable
        filter.cbDesktopSite.isChecked = getTypeMetadata(snapshot).desktopSite
        // todo update web page with debounce. Fix these changes not applying on page reload

        filter.cbUseRelatedLinks.isEnabled = isEditable
        filter.cbUseRelatedLinks.isChecked = getTypeMetadata(snapshot).relatedPageLinksUsed
        filter.cbUsePagination.isEnabled = isEditable
        filter.cbUsePagination.isChecked = getTypeMetadata(snapshot).paginationUsed

        filter.sLinkedPagination.isEnabled = isEditable
        filter.sLinkedPagination.isChecked = isLinkedPagination(snapshot)
        filter.sLinkedPagination.checkedChanges()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    // indexed pagination when editable and not checked
                    filter.etIndexedPaginationPattern.isEnabled = isEditable && !it
                    filter.etIndexedPaginationStartIndex.isEnabled = isEditable && !it
                    filter.etIndexedPaginationStep.isEnabled = isEditable && !it
                    filter.etIndexedPaginationLimit.isEnabled = isEditable && !it
                    // linked pagination when editable and checked
                    filter.etLinkedPaginationLinkFilter.isEnabled = isEditable && it
                    filter.etLinkedPaginationLimit.isEnabled = isEditable && it
                }

        val snapshotPagination = getTypeMetadata(snapshot).pagination
        val initialIndexedPagination = if (snapshotPagination is IndexedPagination)
            snapshotPagination else IndexedPagination()
        val initialLinkedPagination = if (snapshotPagination is LinkedPagination)
            snapshotPagination else LinkedPagination()
        filter.etIndexedPaginationLimit.setText(initialIndexedPagination.limit.toString())
        filter.etIndexedPaginationStep.setText(initialIndexedPagination.step.toString())
        filter.etIndexedPaginationPattern.setText(initialIndexedPagination.path)
        filter.etIndexedPaginationStartIndex.setText(initialIndexedPagination.startIndex.toString())
        filter.etLinkedPaginationLinkFilter.setText(initialLinkedPagination.nextPageLinkFilter)
        filter.etLinkedPaginationLimit.setText(initialLinkedPagination.limit.toString())

        filter.etLoadInterval.isEnabled = isEditable
        filter.etLoadInterval.setText((getTypeMetadata(snapshot).loadIntervalMillis / 1000).toString())

        filter.tvPageLinksPreview.visibility = if (isEditable) View.VISIBLE else View.GONE
        filter.rvRelatedLinkList.visibility = if (isEditable) View.VISIBLE else View.GONE

        filter.tvOfflineLinks.visibility = if (isEditable) View.GONE else View.VISIBLE
        filter.rvOfflineLinkList.visibility = if (isEditable) View.GONE else View.VISIBLE
    }

    /**
     * Performs existing snapshot data loading for viewing.
     *
     * Side effects: reads snapshot data, modifies bound views.
     */
    override fun snapshotViewModeAction(snapshot: SnapshotMetadata) {
        GlobalScope.launch (Dispatchers.Main) {
            content.webView.stopLoading()
            val snapshotPath = dataFolderManager.getSnapshotFolderPath(
                    getDataFolderName(), snapshot.artifactId, snapshot.date)
            val linkToArchivePathRedirectMap = WebArchiveFilesUtil(dataFolderManager, logger)
                    .getPageIndexLinkToArchivePathMap(snapshotPath,
                            "file://$snapshotPath/")
            (filter.rvOfflineLinkList.adapter as OfflineLinkRecyclerAdapter)
                    .setItems(linkToArchivePathRedirectMap
                            .map { OfflineLink(it.key, it.value) })
            val firstArchivePath = linkToArchivePathRedirectMap.entries.firstOrNull()?.value
                    ?: "file:blank" // todo replace
            AndroidWebPageLoader(content.webView).loadHtml(
                    firstArchivePath,
                    linkToArchivePathRedirectMap,
                    getTypeMetadata(snapshot).loadImages,
                    getTypeMetadata(snapshot).desktopSite) {
                android.util.Log.v(this@BlogTypeActivity.TAG, "Loaded HTML from file $firstArchivePath")
                endPreview()
            }
        }
    }

    /**
     * Loads snapshot data preview from the remote source. Before that, changes options when needed.
     *
     * Side effects: shows toast, loads data from source, modifies bound views.
     */
    override fun snapshotCreateModeAction(snapshot: SnapshotMetadata): SnapshotMetadata {
        val snapshotWithNewSettings: SnapshotMetadata

        // disabling pagination by default for manual snapshot creation
        if (getTypeMetadata(snapshot).paginationUsed) {
            snapshotWithNewSettings = snapshot.copy(
                    dataTypeSpecificMetadata = getTypeMetadata(snapshot).copy(
                            paginationUsed = false
                    )
            )
            filter.cbUsePagination.isChecked = false
            Toast.makeText(this, "Pagination is turned off", Toast.LENGTH_SHORT).show()
        } else {
            snapshotWithNewSettings = snapshot
        }

        goToWebPage(snapshot, getTypeMetadata(snapshot).url)
        return snapshotWithNewSettings
    }

    /**
     * Determines if previous content exists relatively to the currently shown content.
     *
     * Side effects: reads bound views.
     */
    override fun contentCanGoBack() = content.webView.canGoBack()

    /**
     * Navigates to the previous content.
     *
     * Side effects: loads data from source (in the corresponding mode), modifies bound views.
     */
    override fun contentGoBack() = content.webView.goBack()

    /**
     * Checks if snapshot metadata is valid. If so then returns snapshot metadata edited up to date.
     *
     * Side effects: reads bound views, shows toast.
     */
    override fun checkSnapshot(snapshot: SnapshotMetadata): Pair<SnapshotMetadata, Boolean> {
        if (controls.etShortUrl.text.trim().isEmpty()) {
            Toast.makeText(this, "Please go to a web page first", Toast.LENGTH_SHORT).show()
            return Pair(snapshot, false)
        }
        return Pair(snapshot.copy(
                dataTypeSpecificMetadata = BlogTypeMetadata(
                        url = if (isSnapshotViewMode()) {
                            getTypeMetadata(snapshot).url // for read only snapshot, same as it was
                        } else if (content.webView.url != null) {
                            content.webView.url.trim('/', ' ') // for editable snapshot, the one from loaded WebView
                        } else {
                            controls.etShortUrl.text.toString().trim('/', ' ')
                        },
                        loadImages = filter.cbLoadImages.isChecked,
                        desktopSite = filter.cbDesktopSite.isChecked,
                        paginationUsed = filter.cbUsePagination.isChecked,
                        pagination = getPaginationFromOptions(),
                        relatedPageLinksUsed = filter.cbUseRelatedLinks.isChecked,
                        relatedPageLinksFilter = filter.etRelatedLinkFilter.text.toString(),
                        relatedPageLinksPattern = filter.etLinkPattern.text.toString(),
                        loadIntervalMillis = filter.etLoadInterval.text.toString().toInt() * 1000L
                )
        ), true)
    }


    // Helpers

    private fun getPaginationFromOptions() = if (filter.sLinkedPagination.isChecked) {
        LinkedPagination(
                nextPageLinkFilter = filter.etLinkedPaginationLinkFilter.text.toString(),
                limit = filter.etLinkedPaginationLimit.text.toString().toInt()
        )
    } else {
        IndexedPagination(
                path = filter.etIndexedPaginationPattern.text.toString(),
                startIndex = filter.etIndexedPaginationStartIndex.text.toString().toInt(),
                step = filter.etIndexedPaginationStep.text.toString().toInt(),
                limit = filter.etIndexedPaginationLimit.text.toString().toInt()
        )
    }

    fun goToOfflinePageDirectly(urlToView: String) {
        content.webView.loadUrl(urlToView) // todo use WebPageLoader; show current URL above when offline or editing
        closeFilterDrawer()
    }

    private fun isLinkedPagination(snapshot: SnapshotMetadata)
            = getTypeMetadata(snapshot).pagination is LinkedPagination

    private fun updateMatchedRelatedLinkList() {
        if (loadedHtml.isEmpty()) { // no links
            (filter.rvRelatedLinkList.adapter as RelatedLinkRecyclerAdapter).setItems(emptyList())
            return
        }
        val allLinkMap = LinkUtil.ccsSelectLinks(loadedHtml, "", "", content.webView.url)
        val matchedLinkMap = LinkUtil.ccsSelectLinks(loadedHtml,
                filter.etLinkPattern.text.toString(),
                filter.etRelatedLinkFilter.text.toString(), content.webView.url)
        val unmatchedLinkMap = allLinkMap.minus(matchedLinkMap)
        // matched shown first
        (filter.rvRelatedLinkList.adapter as RelatedLinkRecyclerAdapter).setItems(
                matchedLinkMap.map {
                    MatchableLink(it.key, it.value, true)
                }.plus(unmatchedLinkMap.map {
                    MatchableLink(it.key as String, it.value, false)
                })
        )
    }

    private var loadedHtml = ""

    private fun getTypeMetadata(snapshot: SnapshotMetadata): BlogTypeMetadata
            = snapshot.dataTypeSpecificMetadata as BlogTypeMetadata

    // URL of first (main) web page of the latest snapshot of this artifact
    private fun getLatestSnapshotUrl(snapshot: SnapshotMetadata) = getTypeMetadata(snapshot).url

    private fun goToWebPage(snapshot: SnapshotMetadata, urlToView: String): Boolean {
        AndroidWebPageLoader(content.webView).loadHtml(LinkUtil.getFullFormUrl(urlToView), emptyMap(),
                getTypeMetadata(snapshot).loadImages, getTypeMetadata(snapshot).desktopSite) {
            android.util.Log.v(this.TAG, "Loaded page from: ${content.webView.url}")
            loadedHtml = it
            // Inputs are pre-filled only when creating new artifact
            if (isArtifactCreateMode()) {
                controls.etShortUrl.text.clear()
                controls.etShortUrl.append(LinkUtil.getShortFormUrl(content.webView.url))
                setTitleInControls(content.webView.title)
                supportActionBar!!.subtitle = content.webView.title
            }
            updateMatchedRelatedLinkList()
        }
        return false
    }
}