package ddwu.com.mobile.example.lbs.placetest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import noman.googleplaces.NRPlaces
import noman.googleplaces.PlaceType
import noman.googleplaces.PlacesException
import noman.googleplaces.PlacesListener
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    /*UI*/
    private var etKeyword: EditText? = null
    private var mGoogleMap: GoogleMap? = null
    private var markerOptions: MarkerOptions? = null

    /*DATA*/
    private var placesClient: PlacesClient? = null

    //위치
    private var current_lat = 37.5178704231165
    private var current_lng = 126.88646244184045

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        etKeyword = findViewById(R.id.etKeyword)
        mapLoad()

//        Places 초기화
        Places.initialize(applicationContext, getString(R.string.api_key))
        placesClient = Places.createClient(this)
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.btnSearch -> {
                if (etKeyword!!.text.toString() == "") {
                    Toast.makeText(this, "'카페'나 '검색'을 입력해주세요", Toast.LENGTH_SHORT).show()
                }
                if (etKeyword!!.text.toString() == "카페") {
                    mGoogleMap!!.clear()
                    searchStart(PlaceType.CAFE)
                } else if (etKeyword!!.text.toString() == "식당") {
                    mGoogleMap!!.clear()
                    searchStart(PlaceType.RESTAURANT)
                }
            }
        }
    }

    /*입력된 유형의 주변 정보를 검색*/
    private fun searchStart(type: String) {
        NRPlaces.Builder()
            .listener(placesListener)
            .key(resources.getString(R.string.api_key))
            .latlng(
                current_lat,
                current_lng
            )
            .radius(500)
            .type(type)
            .build()
            .execute()
    }

    private fun callDetailActivity(place: Place) {
        val intent = Intent(this@MainActivity, DetailActivity::class.java)
        intent.putExtra("name", place.name)
        intent.putExtra("phone", place.phoneNumber)
        intent.putExtra("address", place.address)
        startActivity(intent)
    }

    /*Place ID 의 장소에 대한 세부정보 획득*/
    private fun getPlaceDetail(placeId: String) {
//      상세 정보로 요청할 정보의 유형 지정
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.PHONE_NUMBER,
            Place.Field.ADDRESS
        )
        //       요청 생성
        val request = FetchPlaceRequest.builder(placeId, placeFields).build()
        //       요청 성공시 처리 리스너 연결
        placesClient!!.fetchPlace(request).addOnSuccessListener { response ->
            val place = response.place
            callDetailActivity(place)
            //        요청 실패시 처리 리스너 연결
        }.addOnFailureListener { e ->
            if (e is ApiException) {
                val statusCode = e.statusCode //상태에 따른 코드값 확인
                Log.e(TAG, "Place not found: " + statusCode + " " + e.message)
            }
        }
    }

    var placesListener: PlacesListener = object : PlacesListener {
        override fun onPlacesFailure(e: PlacesException) {}
        override fun onPlacesStart() {}

        // 이 함수가 제일 중요 !
        override fun onPlacesSuccess(places: List<noman.googleplaces.Place>) {
            Log.d(TAG, "Adding Markers")
            runOnUiThread {
                //   마커 추가
                for (place in places) {
                    markerOptions!!.title(place.name)
                    markerOptions!!.position(LatLng(place.latitude, place.longitude))
                    markerOptions!!.icon(
                        BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED
                        )
                    )
                    val newMarker = mGoogleMap!!.addMarker(markerOptions)
                    //   marker의 settag를 사용하여 place id 보관
                    newMarker.tag = place.placeId
                    Log.d(TAG, place.name + " : " + place.placeId)
                }
            }
        }

        override fun onPlacesFinished() {}
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        Log.d(TAG, "Map ready")
        if (checkPermission()) { //permission이 승인되면,
            mGoogleMap!!.isMyLocationEnabled = true
        }
        mGoogleMap!!.setOnMyLocationClickListener { location ->
            Toast.makeText(
                this@MainActivity,
                String.format("현재 위치:(%f, %f)", location.latitude, location.longitude),
                Toast.LENGTH_SHORT
            ).show()
            current_lat = location.latitude
            current_lng = location.longitude
        }
        mGoogleMap!!.setOnMapClickListener { latLng ->
            current_lat = latLng.latitude
            current_lng = latLng.longitude
        }

//    마커의 윈도우를 클릭했을 때 장소의 상세 정보를 보여줌 => getPlaceDetail()
        mGoogleMap!!.setOnInfoWindowClickListener { marker ->
            val placeId = marker.tag.toString()
            getPlaceDetail(placeId)
        }

        //markeroption 준비 => 지도가 준비될 때 load
        markerOptions = MarkerOptions()
    }

    /*구글맵을 멤버변수로 로딩 => onCreate에서 실행 */
    private fun mapLoad() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this) // 매개변수 this: MainActivity 가 OnMapReadyCallback 을 구현하므로
    }

    /* 필요 permission 요청 */
    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQ_CODE
                )
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 퍼미션을 획득하였을 경우 맵 로딩 실행
                mapLoad()
            } else {
                // 퍼미션 미획득 시 액티비티 종료
                Toast.makeText(this, "앱 실행을 위해 권한 허용이 필요함", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        const val TAG = "MainActivity"
        const val PERMISSION_REQ_CODE = 100
    }
}