
package com.screenmask

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAddRule: Button
    private lateinit var adapter: RuleAdapter
    private val ruleManager by lazy { RuleManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerRules)
        btnAddRule = findViewById(R.id.btnAddRule)

        adapter = RuleAdapter(
            rules = ruleManager.getRules().toMutableList(),
            onDelete = { rule ->
                ruleManager.deleteRule(rule.id)
                refreshList()
                updateOverlay()
            },
            onToggle = { rule ->
                ruleManager.updateRule(rule.copy(enabled = !rule.enabled))
                refreshList()
                updateOverlay()
            },
            onModeSwitch = { rule ->
                val newMode =
                    if (rule.mode == Rule.MODE_BLOCK) Rule.MODE_CANVAS else Rule.MODE_BLOCK
                ruleManager.updateRule(rule.copy(mode = newMode))
                refreshList()
                updateOverlay()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnAddRule.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission()
            } else {
                startSelectArea()
            }
        }

        updateOverlay()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun startSelectArea() {
        startActivity(Intent(this, SelectAreaActivity::class.java))
    }

    private fun refreshList() {
        adapter.updateRules(ruleManager.getRules())
    }

    private fun updateOverlay() {
        val intent = Intent(this, OverlayService::class.java)
        stopService(intent)
        if (ruleManager.getRules().any { it.enabled }) {
            startService(intent)
        }
    }

    private fun requestOverlayPermission() {
        AlertDialog.Builder(this)
            .setTitle("需要悬浮窗权限")
            .setMessage("请在设置中允许应用显示悬浮窗")
            .setPositiveButton("去设置") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}

class RuleAdapter(
    private var rules: MutableList<Rule>,
    private val onDelete: (Rule) -> Unit,
    private val onToggle: (Rule) -> Unit,
    private val onModeSwitch: (Rule) -> Unit
) : RecyclerView.Adapter<RuleAdapter.RuleViewHolder>() {

    class RuleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPosition: TextView = view.findViewById(R.id.tvPosition)
        val tvColor: TextView = view.findViewById(R.id.tvColor)
        val viewColorPreview: View = view.findViewById(R.id.viewColorPreview)
        val btnToggle: Button = view.findViewById(R.id.btnToggle)
        val btnMode: Button = view.findViewById(R.id.btnMode)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rule, parent, false)
        return RuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        val rule = rules[position]
        holder.tvPosition.text = "位置: ${rule.left},${rule.top} - ${rule.right},${rule.bottom}"
        holder.tvColor.text = "颜色: #${Integer.toHexString(rule.color).uppercase()}"
        holder.viewColorPreview.setBackgroundColor(rule.color)
        holder.btnToggle.text = if (rule.enabled) "关闭" else "开启"
        holder.btnMode.text = if (rule.mode == Rule.MODE_BLOCK) "方案:块" else "方案:涂抹"

        holder.btnToggle.setOnClickListener { onToggle(rule) }
        holder.btnDelete.setOnClickListener { onDelete(rule) }
        holder.btnMode.setOnClickListener { onModeSwitch(rule) }
    }

    override fun getItemCount() = rules.size

    fun updateRules(newRules: List<Rule>) {
        rules.clear()
        rules.addAll(newRules)
        notifyDataSetChanged()
    }
}