package org.openmaptiles.addons;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.reader.SourceFeature;
import org.openmaptiles.Layer;
import org.openmaptiles.OpenMapTilesProfile;

public class PisteExt implements Layer, OpenMapTilesProfile.OsmAllProcessor {

  private static final String LAYER_NAME = "piste_ext";

  @Override
  public String name() {
    return LAYER_NAME;
  }

  static boolean isLit(SourceFeature feature) {
    Object lit = feature.getTag("lit");
    Object piste_lit = feature.getTag("piste:lit");
    return "yes".equals(lit) || "yes".equals(piste_lit);
  }

  static String getLanes(SourceFeature feature) {
    Object lanes = feature.getTag("piste:classic:lanes");
    Object lanes_alt = feature.getTag("piste:grooming:classic:lanes");

    if (lanes != null) {
      return (String) lanes;
    } else if (lanes_alt != null) {
      return (String) lanes_alt;
    } else {
      return null;
    }
  }

  static String getName(SourceFeature feature) {
    Object name = feature.getTag("name");
    Object piste_name = feature.getTag("piste:name");

    if (name != null) {
      return (String) name;
    } else if (piste_name != null) {
      return (String) piste_name;
    } else {
      return null;
    }
  }

  private static Long getOneway(SourceFeature feature) {
    Object value = feature.getTag("piste:oneway");
    if (value == null) {
      return null;
    } else if (value.equals("yes") || value.equals("1") || value.equals("true")) {
      return 1L;
    } else if (value.equals("no") || value.equals("0") || value.equals("false")) {
      return 0L;
    } else if (value.equals("-1") || value.equals("reverse")) {
      return -1L;
    } else {
      return null;
    }
  }

  @Override
  public void processAllOsm(SourceFeature feature, FeatureCollector features) {
    if (feature.canBeLine() && feature.hasTag("piste:type")) {
      features.line(LAYER_NAME)
        .setBufferPixels(4)
        .setMinZoom(10)
        .setAttr("class", feature.getTag("piste:type"))
        .setAttr("difficulty", feature.getTag("piste:difficulty"))
        .setAttr("grooming", feature.getTag("piste:grooming"))
        .setAttr("oneway", getOneway(feature))
        .setAttr("lit", isLit(feature) ? 1 : null)
        .setAttr("lanes", getLanes(feature))
        .setAttr("name", getName(feature))
        .setAttr("bridge", feature.getTag("bridge"))
        .setAttr("tunnel", feature.getTag("tunnel"))
        .setAttr("mtb_winter", feature.getTag("mtb:winter"))
        .setAttr("foot_winter", feature.getTag("foot:winter"))
        .setAttr("bicycle_winter", feature.getTag("bicycle:winter"));
    }
  }
}
