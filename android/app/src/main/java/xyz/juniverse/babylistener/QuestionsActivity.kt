package xyz.juniverse.babylistener

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.Adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_question.*
import kotlinx.android.synthetic.main.item_qna.view.*
import xyz.juniverse.babylistener.etc.console

/**
 * Created by juniverse on 07/12/2017.
 */
class QuestionsActivity: AppCompatActivity() {

    inner class QnAHolder(private val view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        init {
            console.d("init.... view?", view)
            view.question.setOnClickListener(this)
            view.answer.setOnClickListener(this)
//            getNames(view, "  ")
        }

        override fun onClick(v: View) {
            var myData: Pair<Pair<String, String>, Pair<Boolean, Boolean>> = data ?: return

            when (v.id) {
                R.id.question -> {
                    val visible = !myData.second.first
                    data = Pair(myData.first, Pair(visible, myData.second.second))
                    view.layout_answer.visibility = if (visible) View.VISIBLE else View.GONE
                }

                R.id.answer -> {
                    // todo
                    if (myData.second.second) {
                        startActivity(Intent(baseContext, SettingsActivity::class.java))
                    }
                }
            }
        }

        private var data: Pair<Pair<String, String>, Pair<Boolean, Boolean>>? = null
        fun bind(data: Pair<Pair<String, String>, Pair<Boolean, Boolean>>) {
            console.d("view?", view)
            this.data = data
            with(view) {
                question.text = data.first.first
                answer.text = data.first.second + (if (data.second.second) getString(R.string.click_to_setting) else "")
                layout_answer.visibility = if (data.second.first) View.VISIBLE else View.GONE
            }
        }
    }

    inner class QnAAdapter(private val qnaList:List<Pair<Pair<String, String>, Pair<Boolean, Boolean>>>) : Adapter<QnAHolder>() {

        override fun onBindViewHolder(holder: QnAHolder, position: Int) =
            holder.bind(qnaList[position])

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QnAHolder =
                QnAHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_qna, parent, false))

        override fun getItemCount(): Int = qnaList.size
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_question)

        setSupportActionBar(toolbar)

        val questions = resources.getStringArray(R.array.questions)
        val answers = resources.getStringArray(R.array.answers)
        val clickable = arrayOf(true, true, true, false, true)
        val qnaList: ArrayList<Pair<Pair<String, String>, Pair<Boolean, Boolean>>> = ArrayList()
        (0 until questions.size).mapTo(qnaList) { Pair(Pair(questions[it], answers[it]), Pair(false, clickable[it])) }

        qna_list.layoutManager = LinearLayoutManager(baseContext, LinearLayoutManager.VERTICAL, false)
        qna_list.adapter = QnAAdapter(qnaList)
    }
}