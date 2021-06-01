package ddwu.com.mobile.example.lbs.placetest

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val etName = findViewById<EditText>(R.id.etName)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etAddress = findViewById<EditText>(R.id.etAddress)

        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        etName.setText(intent.getStringExtra("name"))
        etPhone.setText(intent.getStringExtra("phone"))
        etAddress.setText(intent.getStringExtra("address"))

        val clip = ClipData.newPlainText("name", intent.getStringExtra("name"))
        clipboard.setPrimaryClip(clip)
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.btnSave -> Toast.makeText(this, "장소 이름 저장", Toast.LENGTH_SHORT).show()
            R.id.btnClose -> finish()
        }
    }
}