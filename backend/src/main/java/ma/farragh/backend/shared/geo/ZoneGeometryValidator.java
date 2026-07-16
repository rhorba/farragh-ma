package ma.farragh.backend.shared.geo;

import ma.farragh.backend.shared.exception.BusinessException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Shared polygon/point-radius validation for any module that lets a user declare a
 * geographic zone (recyclers' coverage zone, municipality's bulk-subscription zone).
 * Kept here rather than in either feature package so neither module needs a direct
 * dependency on the other's internals.
 */
@Component
public class ZoneGeometryValidator {

    private static final int WGS84_SRID = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), WGS84_SRID);

    public record ZoneGeometry(Polygon area, Point centerPoint, Integer radiusM) {
    }

    public record ZoneCoordinates(Double centerLatitude, Double centerLongitude, List<List<Double>> polygon) {
    }

    /** Maps a persisted zone's raw geometry columns back to API-friendly lat/lng/polygon fields. */
    public static ZoneCoordinates toCoordinates(Point centerPoint, Polygon area) {
        Double lat = centerPoint != null ? centerPoint.getY() : null;
        Double lng = centerPoint != null ? centerPoint.getX() : null;
        List<List<Double>> polygonCoords = area != null
                ? Arrays.stream(area.getCoordinates()).map(c -> List.of(c.x, c.y)).toList()
                : null;
        return new ZoneCoordinates(lat, lng, polygonCoords);
    }

    public ZoneGeometry resolve(Double centerLatitude, Double centerLongitude, Integer radiusM,
                                 List<List<Double>> polygon) {
        if (polygon != null && !polygon.isEmpty()) {
            return new ZoneGeometry(buildValidatedPolygon(polygon), null, null);
        } else if (centerLatitude != null && centerLongitude != null && radiusM != null) {
            validateLatLng(centerLatitude, centerLongitude);
            Point centerPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(centerLongitude, centerLatitude));
            return new ZoneGeometry(null, centerPoint, radiusM);
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST, "ZONE_GEOMETRY_REQUIRED",
                "Provide either a polygon or a center point with radiusM.");
    }

    public Polygon buildValidatedPolygon(List<List<Double>> points) {
        if (points.size() < 4) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_ZONE_GEOMETRY",
                    "A zone polygon needs at least 4 points forming a closed ring.");
        }

        Coordinate[] coords = points.stream()
                .map(p -> {
                    if (p.size() != 2) {
                        throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_ZONE_GEOMETRY",
                                "Each polygon point must be a [longitude, latitude] pair.");
                    }
                    validateLatLng(p.get(1), p.get(0));
                    return new Coordinate(p.get(0), p.get(1));
                })
                .toArray(Coordinate[]::new);

        if (!coords[0].equals2D(coords[coords.length - 1])) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_ZONE_GEOMETRY",
                    "Polygon ring must be closed (first and last points equal).");
        }

        Polygon polygon;
        try {
            LinearRing ring = GEOMETRY_FACTORY.createLinearRing(coords);
            polygon = GEOMETRY_FACTORY.createPolygon(ring);
        } catch (RuntimeException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_ZONE_GEOMETRY",
                    "The zone polygon geometry is invalid.");
        }

        if (!polygon.isValid()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_ZONE_GEOMETRY",
                    "The zone polygon is invalid or self-intersecting.");
        }
        return polygon;
    }

    public void validateLatLng(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_COORDINATES",
                    "Latitude must be between -90 and 90, longitude between -180 and 180.");
        }
    }
}
