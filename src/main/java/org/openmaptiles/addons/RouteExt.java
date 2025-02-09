package org.openmaptiles.addons;

import static org.openmaptiles.util.Utils.coalesce;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.FeatureMerge;
import com.onthegomap.planetiler.VectorTile;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.reader.osm.OsmElement;
import com.onthegomap.planetiler.reader.osm.OsmRelationInfo;
import java.util.List;
import org.openmaptiles.Layer;
import org.openmaptiles.OpenMapTilesProfile;

public class RouteExt implements Layer, OpenMapTilesProfile.OsmAllProcessor,
  OpenMapTilesProfile.OsmRelationPreprocessor, OpenMapTilesProfile.FeaturePostProcessor {

  private static final String LAYER_NAME = "route_ext";

  @Override
  public String name() {
    return LAYER_NAME;
  }

  @Override
  public List<OsmRelationInfo> preprocessOsmRelation(OsmElement.Relation relation) {
    if (relation.hasTag("route", "hiking") || relation.hasTag("route", "bicycle", "mtb")) {
      String network = relation.getString("network");
      String ref = relation.getString("ref");
      String name = relation.getString("name");
      String route = relation.getString("route");

      return List.of(new RouteRelation(name, coalesce(ref, ""), route, network, relation.id()));
    }
    return null;
  }

  @Override
  public void processAllOsm(SourceFeature feature, FeatureCollector features) {
    for (var routeInfo : feature.relationInfo(RouteRelation.class)) {
      // (routeInfo.role() also has the "role" of this relation member if needed)
      RouteRelation relation = routeInfo.relation();
      // Break the output into layers named: "{bicycle,route}-route-{international,national,regional,local,other}"
      //String layerName = relation.route + "-route-" + relation.network;
      features.line(LAYER_NAME)
        .setAttr("name", relation.name)
        .setAttr("ref", relation.ref)
        .setAttr("route", relation.route)
        .setAttr("network", relation.network)
        .setAttr("id", relation.id)
        .setZoomRange(0, 14)
        // don't filter out short line segments even at low zooms because the next step needs them
        // to merge lines with the same tags where the endpoints are touching
        .setMinPixelSize(0);
    }
  }

  @Override
  public List<VectorTile.Feature> postProcess(int zoom, List<VectorTile.Feature> items) {
    return FeatureMerge.mergeLineStrings(items,
      0.5, // after merging, remove lines that are still less than 0.5px long
      0.1, // simplify output linestrings using a 0.1px tolerance
      4 // remove any detail more than 4px outside the tile boundary
    );
  }

  record RouteRelation(
    String name,
    String ref,
    String route,
    String network,
    @Override long id
  ) implements OsmRelationInfo {}
}
