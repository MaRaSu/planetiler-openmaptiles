package org.openmaptiles.addons;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.reader.SourceFeature;
import java.util.List;
import java.util.Set;
import org.openmaptiles.Layer;
import org.openmaptiles.OpenMapTilesProfile;

public class PoiExt implements Layer, OpenMapTilesProfile.OsmAllProcessor {

  private static final String LAYER_NAME = "poi_ext";

  private static final int DEFAULT_MIN_ZOOM = 9;
  private static final int WATER_EXT_MIN_ZOOM = 10;

  private static final Set<String> ACCESS_NO_VALUES = Set.of(
    "private", "no", "military", "permit", "delivery", "customers"
  );

  /**
   * One tag combination ("key=value,key2=value2") mapped to "class,subclass".
   */
  private record Rule(String[][] tags, String clazz, String subclass, int minZoom) {

    static Rule of(String tagCombination, String classSubclass) {
      return of(tagCombination, classSubclass, DEFAULT_MIN_ZOOM);
    }

    static Rule of(String tagCombination, String classSubclass, int minZoom) {
      String[] tagList = tagCombination.split(",");
      String[][] tags = new String[tagList.length][];
      for (int i = 0; i < tagList.length; i++) {
        tags[i] = tagList[i].split("=");
      }
      String[] cs = classSubclass.split(",");
      return new Rule(tags, cs[0], cs.length > 1 ? cs[1] : null, minZoom);
    }

    boolean matches(SourceFeature feature) {
      for (String[] keyValue : tags) {
        if (!feature.hasTag(keyValue[0], keyValue.length > 1 ? keyValue[1] : null)) {
          return false;
        }
      }
      return true;
    }
  }

  // The first matching rule wins, so the order matters. Non-water rules are listed in the order the former
  // HashMap iterated them, which is what decided the class until now. Water rules form one block placed where
  // amenity=drinking_water used to be, ordered by priority among themselves.
  private static final List<Rule> RULES = List.of(
    Rule.of("tourism=viewpoint", "tourism,viewpoint"),
    Rule.of("amenity=cafe", "amenity,cafe"),
    Rule.of("amenity=toilets", "amenity,toilets"),
    Rule.of("tourism=picnic_site", "tourism,picnic_site"),
    Rule.of("tourism=camp_pitch", "tourism,camp_pitch"),
    Rule.of("amenity=pub", "amenity,pub"),
    Rule.of("amenity=shelter,shelter_type=picnic_shelter", "shelter,picnic_shelter"),
    Rule.of("leisure=swimming_area", "leisure,swimming_area"),
    Rule.of("natural=hill", "natural,hill"),
    Rule.of("shop=ice_cream", "shop,ice_cream"),
    Rule.of("amenity=shelter,shelter_type=basic_hut", "shelter,lean_to"),
    Rule.of("tourism=camp_site", "tourism,camp_site"),
    Rule.of("obstacle=fallen_tree", "obstacle,fallen_tree"),
    Rule.of("shop=mall", "shop,mall"),
    Rule.of("shop=department_store", "shop,department_store"),
    Rule.of("natural=hills", "natural,hills"),
    Rule.of("place=archipelago", "place,archipelago"),
    Rule.of("tourism=motel", "tourism,motel"),
    Rule.of("man_made=tower,tower:type=observation", "tower,observation"),
    Rule.of("leisure=firepit", "leisure,firepit"),
    Rule.of("shop=supermarket", "shop,supermarket"),
    Rule.of("amenity=bicycle_repair_station", "amenity,bicycle_repair_station"),
    Rule.of("tourism=chalet", "tourism,chalet"),
    Rule.of("amenity=fast_food", "amenity,fast_food"),
    Rule.of("tourism=wilderness_hut", "tourism,wilderness_hut"),
    Rule.of("ford=no", "ford,no"),
    Rule.of("amenity=bar", "amenity,bar"),
    Rule.of("leisure=park", "leisure,park"),
    Rule.of("historic=memorial", "historic,memorial"),
    Rule.of("noexit=yes", "noexit,yes"),
    Rule.of("shop=bakery", "shop,bakery"),
    Rule.of("shop=coffee", "shop,coffee"),
    Rule.of("tourism=artwork", "tourism,artwork"),
    Rule.of("military=bunker", "military,bunker"),
    Rule.of("shop=food", "shop,food"),
    Rule.of("building=toilets", "amenity,toilets"),
    Rule.of("natural=cave_entrance", "natural,cave_entrance"),
    Rule.of("shop=general", "shop,general"),
    Rule.of("leisure=picnic_table", "leisure,picnic_table"),
    Rule.of("historic=monument", "historic,monument"),
    Rule.of("amenity=shelter,shelter_type=weather_shelter", "shelter,weather_shelter"),
    Rule.of("tourism=hotel", "tourism,hotel"),
    Rule.of("shop=convenience", "shop,convenience"),
    Rule.of("natural=isthmus", "natural,isthmus"),
    Rule.of("shop=pastry", "shop,pastry"),
    Rule.of("amenity=shelter,shelter_type=gazebo", "shelter,gazebo"),
    Rule.of("place=square", "place,square"),
    Rule.of("tourism=attraction", "tourism,attraction"),
    Rule.of("amenity=restaurant", "amenity,restaurant"),
    Rule.of("natural=peninsula", "natural,peninsula"),
    Rule.of("ford=yes", "ford,yes"),
    // water block
    Rule.of("amenity=drinking_water", "water,drinking_water"),
    Rule.of("amenity=water_point", "water,water_point"),
    Rule.of("natural=spring", "water,spring", WATER_EXT_MIN_ZOOM),
    Rule.of("man_made=water_tap,drinking_water=yes", "water,water_tap", WATER_EXT_MIN_ZOOM),
    Rule.of("amenity=fountain,drinking_water=yes", "water,fountain", WATER_EXT_MIN_ZOOM),
    Rule.of("man_made=water_well,drinking_water=yes", "water,water_well", WATER_EXT_MIN_ZOOM),
    // end of water block
    Rule.of("tourism=hostel", "tourism,hostel"),
    Rule.of("tourism=apartment", "tourism,apartment"),
    Rule.of("building=bunker", "building,bunker"),
    Rule.of("landuse=military", "landuse,military"),
    Rule.of("natural=peak", "natural,peak"),
    Rule.of("amenity=shelter,shelter_type=lean_to", "shelter,lean_to")
  );

  @Override
  public String name() {
    return LAYER_NAME;
  }

  @Override
  public void processAllOsm(SourceFeature feature, FeatureCollector features) {
    // Handle "fixme" tag specifically
    if (feature.hasTag("fixme")) {
      String initialMethod = determineInitialMethod(feature);
      String subclassValue = (String) feature.getTag("fixme");
      addFeature(features, initialMethod, "fixme", subclassValue, DEFAULT_MIN_ZOOM, feature);
      return; // Skip further processing if "fixme" is handled
    }

    for (Rule rule : RULES) {
      if (rule.matches(feature)) {
        addFeature(features, determineInitialMethod(feature), rule.clazz(), rule.subclass(), rule.minZoom(), feature);
        break; // One feature per OSM object: the first matching rule wins
      }
    }
  }

  private String determineInitialMethod(SourceFeature feature) {
    if (!(feature.canBeLine() || feature.canBePolygon())) {
      return "point";
    } else if (feature.canBePolygon()) {
      return "centroidIfConvex";
    }
    return null; // or some default action
  }

  private void addFeature(FeatureCollector features, String initialMethod, String clazz, String subclass,
    int minZoom, SourceFeature feature) {
    com.onthegomap.planetiler.FeatureCollector.Feature finalFeature;

    Object access = feature.getTag("access");
    if (access != null && ACCESS_NO_VALUES.contains(access)) {
      return;
    }

    if ("point".equals(initialMethod)) {
      finalFeature = features.point(LAYER_NAME);
    } else if ("centroidIfConvex".equals(initialMethod)) {
      finalFeature = features.centroidIfConvex(LAYER_NAME);
    } else {
      return; // or some default action
    }

    finalFeature.setBufferPixels(4)
      .setMinZoom(minZoom)
      .setAttr("class", clazz);
    if (subclass != null && !subclass.isEmpty()) {
      finalFeature.setAttr("subclass", subclass)
        .setAttr("access", feature.getTag("access"))
        .setAttr("name", feature.getTag("name"))
        .setAttr("website", feature.getTag("website"))
        .setAttr("url", feature.getTag("url"));
    }
    if ("water".equals(clazz)) {
      finalFeature.setAttr("drinking_water", feature.getTag("drinking_water"));
    }
  }
}
