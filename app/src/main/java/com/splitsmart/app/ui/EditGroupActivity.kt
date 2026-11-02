package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.splitsmart.app.R
import com.splitsmart.app.business.GroupManager
import com.splitsmart.app.databinding.ActivityEditGroupBinding

class EditGroupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditGroupBinding
    private val groupManager = GroupManager.getInstance()
    private lateinit var groupId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.edit_group)

        groupId = intent.getStringExtra(HomeActivity.EXTRA_GROUP_ID).orEmpty()
        if (groupId.isBlank()) {
            toast("Missing group")
            finish()
            return
        }

        launchIo {
            val group = groupManager.getGroup(groupId)
            runOnUiThread {
                binding.etName.setText(group.name)
                binding.etDescription.setText(group.description)
            }
        }

        binding.btnSave.setOnClickListener {
            binding.progress.visibility = View.VISIBLE
            launchIo(onError = {
                binding.progress.visibility = View.GONE
                toast(it)
            }) {
                groupManager.editGroup(
                    groupId,
                    binding.etName.text?.toString().orEmpty(),
                    binding.etDescription.text?.toString().orEmpty()
                )
                runOnUiThread {
                    binding.progress.visibility = View.GONE
                    toast("Saved")
                    finish()
                }
            }
        }

        binding.btnDelete.setOnClickListener {
            confirm("Delete group", "Delete this group and all expenses/settlements?") {
                binding.progress.visibility = View.VISIBLE
                launchIo(onError = {
                    binding.progress.visibility = View.GONE
                    toast(it)
                }) {
                    groupManager.deleteGroup(groupId)
                    runOnUiThread {
                        binding.progress.visibility = View.GONE
                        startActivity(Intent(this@EditGroupActivity, HomeActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        })
                        finish()
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
