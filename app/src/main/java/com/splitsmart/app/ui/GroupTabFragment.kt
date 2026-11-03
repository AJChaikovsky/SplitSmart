package com.splitsmart.app.ui

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Work belongs to the view, so a destroyed tab or sheet cannot update stale controls. */
fun <T> Fragment.loadData(
    fetch: suspend () -> T,
    render: (T) -> Unit,
    onError: (String) -> Unit = { requireContext().toast(it) }
): Job = viewLifecycleOwner.lifecycleScope.launch {
    try {
        val result = withContext(Dispatchers.IO) { fetch() }
        render(result)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        onError(error.message ?: error.toString())
    }
}

abstract class GroupTabFragment(layoutId: Int) : Fragment(layoutId) {
    protected val groupId: String
        get() = requireArguments().getString(HomeActivity.EXTRA_GROUP_ID).orEmpty()

    abstract fun refresh()

    override fun onResume() {
        super.onResume()
        refresh()
    }
}
