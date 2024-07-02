package com.example.atividadearcgis;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.MapView;

import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private MapView mMapView;
    private FeatureLayer shapefileFeatureLayer;
    private Callout mCallout;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMapView = findViewById(R.id.mapView);

        setupMapHidrografia();
        mCallout = mMapView.getCallout();

        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (mCallout.isShowing()) {
                    mCallout.dismiss();
                }

                final Point screenPoint = new Point(Math.round(e.getX()), Math.round(e.getY()));
                int tolerance = 10;

                final ListenableFuture<IdentifyLayerResult> identifyLayerResultListenableFuture;
                identifyLayerResultListenableFuture = mMapView.identifyLayerAsync(shapefileFeatureLayer, screenPoint, tolerance, false, 1);

                identifyLayerResultListenableFuture.addDoneListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            IdentifyLayerResult identifyLayerResult = identifyLayerResultListenableFuture.get();
                            TextView calloutContent = new TextView(getApplicationContext());
                            calloutContent.setTextColor(Color.BLACK);
                            calloutContent.setSingleLine(false);
                            calloutContent.setVerticalScrollBarEnabled(true);
                            calloutContent.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
                            calloutContent.setMovementMethod(new ScrollingMovementMethod());

                            for (GeoElement element : identifyLayerResult.getElements()) {
                                Feature feature = (Feature) element;
                                Map<String, Object> attr = feature.getAttributes();

                                Set<String> keys = attr.keySet();

                                for (String key : keys) {
                                    Object value = attr.get(key);
                                    calloutContent.append(key + "|" + value + "\n");
                                }
                                Envelope envelope = feature.getGeometry().getExtent();
                                mMapView.setViewpointGeometryAsync(envelope, 200);

                                mCallout.setLocation(envelope.getCenter());
                                mCallout.setContent(calloutContent);

                                mCallout.show();
                            }

                        } catch (Exception e1) {
                            Log.e(getResources().getString(R.string.app_name), "Select feature failed: " + e1.getMessage());
                        }
                    }
                });

                return super.onSingleTapConfirmed(e);
            }
        });
    }

    private void setupMapHidrografia() {
        try {
            ArcGISRuntimeEnvironment.setApiKey("AAPT3NKHt6i2urmWtqOuugvr9VaHGvqwPdEBvujs7twkXt2mGmQrx_F7ee0_qz-QOUianK4FnMPqsm4DLXGoxfPCuQVRoZPKC8tbVgocGKhYrZ7hxgbM5eejm-0jLigqPfRfV0epZ6X27ouv0SaWwPEUBRUh_72j5LMZ7GaZ4AAOONSj2QYXT-psAfPX951NVzgPbTZzjpHNbPmLczZ8bi9OpJJmJNuwOq090-5mt6Kxy1FLeNS3x1ZHCF9SkMib98-R");

            ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC);
            mMapView.setMap(map);

            String serviceUrl = "https://services3.arcgis.com/eT5XKLXThq6BSeoM/arcgis/rest/services/Doce/FeatureServer/0";
            ServiceFeatureTable shapefileFeatureTable = new ServiceFeatureTable(serviceUrl);

            Log.d("URL", "Service URL: " + shapefileFeatureTable.getUri());

            shapefileFeatureLayer = new FeatureLayer(shapefileFeatureTable);

            map.getOperationalLayers().add(shapefileFeatureLayer);

            shapefileFeatureLayer.addDoneLoadingListener(() -> {
                if (shapefileFeatureLayer.getLoadStatus() == com.esri.arcgisruntime.loadable.LoadStatus.LOADED) {
                    Toast.makeText(this, "Camada carregada com sucesso", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("MapError", "Erro ao carregar a camada: " + shapefileFeatureLayer.getLoadError().getMessage());
                    Toast.makeText(this, "Erro ao carregar a camada", Toast.LENGTH_SHORT).show();
                }
            });

            map.addDoneLoadingListener(() -> {
                if (map.getLoadStatus() == com.esri.arcgisruntime.loadable.LoadStatus.LOADED) {
                    Toast.makeText(this, "Mapa carregado com sucesso", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("MapError", "Erro ao carregar o mapa: " + map.getLoadError().getMessage());

                    if (map.getLoadError() != null && map.getLoadError().getCause() != null) {
                        Throwable rootCause = map.getLoadError().getCause();
                        Log.e("MapError", "Causa raiz do erro: " + rootCause.getMessage());
                        rootCause.printStackTrace();
                    }

                    Toast.makeText(this, "Erro ao carregar o mapa", Toast.LENGTH_SHORT).show();
                }
            });

            mMapView.setViewpoint(new Viewpoint(-20.0, -41.0, 2500000.0));
        } catch (Exception e) {
            Log.e("MapError", "Erro ao configurar o mapa: ", e);
            Toast.makeText(this, "Erro ao configurar o mapa", Toast.LENGTH_LONG).show();
        }
    }
}
