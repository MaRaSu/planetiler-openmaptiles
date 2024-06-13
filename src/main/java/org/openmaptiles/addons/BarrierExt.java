package org.openmaptiles.addons;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.reader.SourceFeature;
import org.openmaptiles.Layer;
import org.openmaptiles.OpenMapTilesProfile;

public class BarrierExt implements Layer, OpenMapTilesProfile.OsmAllProcessor {

  private static final String LAYER_NAME = "barrier_ext";

  @Override
  public String name() {
    return LAYER_NAME;
  }

  @Override
  public void processAllOsm(SourceFeature feature, FeatureCollector features) {
    if (feature.canBeLine() && feature.hasTag("barrier")) {
      features.line(LAYER_NAME)
          .setBufferPixels(4)
          .setMinZoom(12)
          .setAttr("class", feature.getTag("barrier"))
          .setAttr("access", feature.getTag("access"));
    }
  }
}