package org.openmaptiles.addons;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.reader.SourceFeature;
import org.openmaptiles.Layer;
import org.openmaptiles.OpenMapTilesProfile;

public class PoiExt implements Layer, OpenMapTilesProfile.OsmAllProcessor {

  private static final String LAYER_NAME = "poi_ext";

  @Override
  public String name() {
    return LAYER_NAME;
  }

  @Override
  public void processAllOsm(SourceFeature feature, FeatureCollector features) {
    if (!(feature.canBeLine() || feature.canBePolygon())) {
			if (feature.hasTag("noexit", "yes")) {
      features.point(LAYER_NAME)
          .setBufferPixels(4)
          .setMinZoom(13)
          .setAttr("class", "noexit");
    } else if (feature.hasTag("fixme")) {
			    features.point(LAYER_NAME)
          .setBufferPixels(4)
          .setMinZoom(13)
          .setAttr("class", "fixme")
					.setAttr("fixme", feature.getTag("fixme"));
			}
		}
	}
}
