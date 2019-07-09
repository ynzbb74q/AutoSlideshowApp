package jp.techacademy.takayuki.ochiai.autoslideshowapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.content.ContentUris
import android.net.Uri
import android.view.View
import android.preference.PreferenceManager
import android.os.Handler
import java.util.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    // パーミッション応答コード
    private val PERMISSIONS_REQUEST_CODE = 100

    // 現在表示している画像番号
    private var IMAGE_DISPLAY_INDEX = 0

    // 保存されている画像枚数
    private var IMAGE_COUNT = 0

    // 「再生/停止」判定(true:再生中 / false:停止中)
    private var IS_AUTO = false

    // タイマー変数
    private var mTimer: Timer? = null
    private var mHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // パーミッション許可確認判定
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                getImage()
                setImage(0)
            } else {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)
            }
        } else {
            getImage()
            setImage(0)
        }

        buttonNext.setOnClickListener(this)
        buttonPrev.setOnClickListener(this)
        buttonAuto.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.buttonNext -> doNext()
            R.id.buttonPrev -> doPrev()
            R.id.buttonAuto -> doAuto()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getImage()
                    setImage(0)
                }
        }
    }

    // 「進む」ボタン押下時処理
    private fun doNext() {
        if (IMAGE_DISPLAY_INDEX == IMAGE_COUNT - 1) {
            IMAGE_DISPLAY_INDEX = 0
        } else {
            IMAGE_DISPLAY_INDEX++
        }
        setImage(IMAGE_DISPLAY_INDEX)
    }

    // 「戻る」ボタン押下時処理
    private fun doPrev() {
        if (IMAGE_DISPLAY_INDEX == 0) {
            IMAGE_DISPLAY_INDEX = IMAGE_COUNT - 1
        } else {
            IMAGE_DISPLAY_INDEX--
        }
        setImage(IMAGE_DISPLAY_INDEX)
    }

    // 「再生/停止」ボタン押下時処理
    private fun doAuto() {
        // 状態切り替え
        IS_AUTO = !IS_AUTO

        // スライドショー実行/停止
        doSlideshow()

        // 「再生/停止」ボタンの表示切り替え
        if (IS_AUTO) {
            buttonAuto.text = "停止"
            // 「進む」「戻る」ボタンを無効化
            buttonNext.setEnabled(false)
            buttonPrev.setEnabled(false)
        } else {
            buttonAuto.text = "再生"
            // 「進む」「戻る」ボタンを有効化
            buttonNext.setEnabled(true)
            buttonPrev.setEnabled(true)
        }
    }

    // 画像を取得しPreferenceに保存
    private fun getImage() {
        val resolver = contentResolver
        val cursor = resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            null
        )

        if (cursor == null) {
            return
        }

        // 画像枚数特定
        IMAGE_COUNT = cursor.count

        // 画像URIをPreferenceに保存(事前に保存されていた画像URIを全て削除してから再保存する)
        val preference = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preference.edit()
        editor.clear()
        if (cursor.moveToFirst()) {
            var index = 0
            do {
                val fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val id = cursor.getLong(fieldIndex)
                val imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                // IMAGE_0, IMAGE_1, ...のキーで画像URIを保存
                editor.putString("IMAGE_${index}", imageUri.toString())
                editor.commit()

                index++
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    // 画像を表示
    private fun setImage(index: Int) {
        // 引数チェック
        if (index < 0 || index > IMAGE_COUNT - 1) {
            return
        }

        val preference = PreferenceManager.getDefaultSharedPreferences(this)
        val imageUri = preference.getString("IMAGE_${index}", "")

        // 画像有無チェック
        if (imageUri.isNullOrEmpty()) {
            return
        }

        // 画像設定
        imageView.setImageURI(Uri.parse(imageUri))
    }

    // スライドショー再生・停止
    private fun doSlideshow() {
        if (IS_AUTO && mTimer == null) {
            mTimer = Timer()
            mTimer!!.schedule(object : TimerTask() {
                override fun run() {
                    mHandler.post {
                        if (IMAGE_DISPLAY_INDEX == IMAGE_COUNT - 1) {
                            IMAGE_DISPLAY_INDEX = 0
                        } else {
                            IMAGE_DISPLAY_INDEX++
                        }
                        setImage(IMAGE_DISPLAY_INDEX)
                    }
                }
            }, 2000, 2000)
        } else {
            if (mTimer != null) {
                mTimer!!.cancel()
                mTimer = null
            }
        }
    }
}
