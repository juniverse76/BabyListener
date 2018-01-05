package xyz.juniverse.babylistener.ui.intro

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import xyz.juniverse.babylistener.R
import xyz.juniverse.babylistener.etc.console

/**
 * Created by juniverse on 04/01/2018.
 */
open class IntroFragment : Fragment(), View.OnClickListener {
    companion object {
        val TAG = "intro.frag.tag"

        private val ARG_FRAG_VIEW_ID = "frag.view.id"
        fun create(viewId: Int): IntroFragment {
            val fragment = when (viewId) {
                R.layout.frag_pair_request -> PairRequestFragment()
                R.layout.frag_pair_respond -> PairRespondFragment()
                else -> IntroFragment()
            }

            val args = Bundle()
            args.putInt(ARG_FRAG_VIEW_ID, viewId)
            fragment.arguments = args
            return fragment
        }

        // kinda automation???
        private val moveFragmentMap: HashMap<Int, Int> = hashMapOf(
            R.id.to_select_mode to R.layout.frag_select_mode,
                R.id.to_pair_mode to R.layout.frag_pair_method,
                R.id.to_call_mode to R.layout.frag_call_mode,
                R.id.to_pair_request to R.layout.frag_pair_request,
                R.id.to_pair_respond to R.layout.frag_pair_respond
        )
    }

    private var myViewId: Int = 0
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        myViewId = arguments.getInt(ARG_FRAG_VIEW_ID)
        val view = inflater.inflate(myViewId, container, false)
        registerButtons(view)
        return view
    }

    protected fun startFragment(fragment: Fragment) {
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_from_right, R.anim.slide_to_left, R.anim.slide_from_left, R.anim.slide_to_right)
                .addToBackStack("")
                .replace(R.id.fragment_holder, fragment, TAG)
                .commit()
    }

    private fun registerButtons(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                registerButtons(view.getChildAt(i))
            }
        } else if (view is Button || view is ImageButton) {
            view.setOnClickListener(this)
        }
    }

    override fun onClick(view: View) {
        console.d("unhandled button", view.id)

        moveFragmentMap[view.id]?.let { viewId ->
            startFragment(create(viewId))
        }
    }

    open fun onBackPressed(): Boolean {
        return false
    }
}