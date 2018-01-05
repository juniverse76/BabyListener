package xyz.juniverse.babylistener

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import xyz.juniverse.babylistener.etc.console

/**
 * Created by juniverse on 28/11/2017.
 */
class ContactAdapter(context: Context?, list: ArrayList<MainActivity.Contact>) : ArrayAdapter<MainActivity.Contact>(context, R.layout.item_contact, list) {
    private val maxSize = 5

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view : View = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false)

        val contact = getItem(position)
        (view.findViewById<TextView>(R.id.name)).text = contact.name
        (view.findViewById<TextView>(R.id.code)).text = String.format("(%s)", contact.number)

        return view
    }

    override fun getFilter(): Filter = filter

    val suggestion = ArrayList<MainActivity.Contact>()

    private val filter = object: Filter() {
        override fun performFiltering(charSequence: CharSequence?): FilterResults {
            val result = FilterResults()
            if (charSequence != null) {
                suggestion.clear()
                for (contact in list) {
                    if (contact.name.contains(charSequence) || contact.number.contains(charSequence)) {
                        suggestion.add(contact)
                        result.count++
                    }
                    if (result.count >= maxSize)
                        break
                }
                result.values = suggestion
            }
            return result
        }

        override fun publishResults(charSequence: CharSequence?, filterResults: FilterResults?) {
            if (filterResults == null || filterResults.count <= 0)
                return

            clear()
            val filtered = filterResults.values as ArrayList<MainActivity.Contact>
            for (contact in filtered)
                add(contact)
            notifyDataSetChanged()
        }

        override fun convertResultToString(resultValue: Any?): CharSequence {
            console.d("convertResultToString")
            val contact = resultValue as? MainActivity.Contact
            return contact?.number ?: ""
        }
    }
}
