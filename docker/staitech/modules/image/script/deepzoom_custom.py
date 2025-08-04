
from openslide.deepzoom import DeepZoomGenerator
from PIL import Image
import math
import openslide

class DeepZoomGeneratorCustom(DeepZoomGenerator):
    def __init__(self, osr, tile_size=254, overlap=1, limit_bounds=False, level_tiles=None):
        """Generates Deep Zoom tiles and metadata."""

    BOUNDS_OFFSET_PROPS = (
        openslide.PROPERTY_NAME_BOUNDS_X,
        openslide.PROPERTY_NAME_BOUNDS_Y,
    )
    BOUNDS_SIZE_PROPS = (
        openslide.PROPERTY_NAME_BOUNDS_WIDTH,
        openslide.PROPERTY_NAME_BOUNDS_HEIGHT,
    )

    def __init__(
        self,
        osr: openslide.AbstractSlide,
        tile_size: int = 254,
        overlap: int = 1,
        limit_bounds: bool = False,
    ):
        """
        Create a DeepZoomGeneratorCustom extends the DeepZoomGenerator .

        改造级别计算逻辑,使用tile_size计算级别(默认是1 px),适配前端
        z_dimensions = [z_size]
        while z_size[0] > tile_size or z_size[1] > tile_size:
            z_size = tuple(max(1, int(math.ceil(z / 2))) for z in z_size)
            if z_size[0] < tile_size or z_size[1] < tile_size: break
            z_dimensions.append(z_size)

        osr:          a slide object.
        tile_size:    the width and height of a single tile.  For best viewer
                      performance, tile_size + 2 * overlap should be a power
                      of two.
        overlap:      the number of extra pixels to add to each interior edge
                      of a tile.
        limit_bounds: True to render only the non-empty slide region.
        """

        # We have four coordinate planes:
        # - Row and column of the tile within the Deep Zoom level (t_)
        # - Pixel coordinates within the Deep Zoom level (z_)
        # - Pixel coordinates within the slide level (l_)
        # - Pixel coordinates within slide level 0 (l0_)

        self._osr = osr
        self._z_t_downsample = tile_size
        self._z_overlap = overlap
        self._limit_bounds = limit_bounds

        # Precompute dimensions
        # Slide level and offset
        if limit_bounds:
            # Level 0 coordinate offset
            self._l0_offset = tuple(
                int(osr.properties.get(prop, 0)) for prop in self.BOUNDS_OFFSET_PROPS
            )
            # Slide level dimensions scale factor in each axis
            size_scale = tuple(
                int(osr.properties.get(prop, l0_lim)) / l0_lim
                for prop, l0_lim in zip(self.BOUNDS_SIZE_PROPS, osr.dimensions)
            )
            # Dimensions of active area
            self._l_dimensions = tuple(
                tuple(
                    int(math.ceil(l_lim * scale))
                    for l_lim, scale in zip(l_size, size_scale)
                )
                for l_size in osr.level_dimensions
            )
        else:
            self._l_dimensions = osr.level_dimensions
            self._l0_offset = (0, 0)
        self._l0_dimensions = self._l_dimensions[0]
        # Deep Zoom level
        z_size = self._l0_dimensions
        z_dimensions = [z_size]
        while z_size[0] > tile_size or z_size[1] > tile_size:
            z_size = tuple(max(1, int(math.ceil(z / 2))) for z in z_size)
            if z_size[0] < tile_size or z_size[1] < tile_size: break
            z_dimensions.append(z_size)
        # Narrow the type, for self.level_dimensions
        self._z_dimensions = self._pairs_from_n_tuples(tuple(reversed(z_dimensions)))

        # Tile
        def tiles(z_lim: int) -> int:
            return int(math.ceil(z_lim / self._z_t_downsample))

        self._t_dimensions = tuple(
            (tiles(z_w), tiles(z_h)) for z_w, z_h in self._z_dimensions
        )

        # Deep Zoom level count
        self._dz_levels = len(self._z_dimensions)

        # Total downsamples for each Deep Zoom level
        # mypy infers this as a tuple[Any, ...] due to the ** operator
        l0_z_downsamples: tuple[int, ...] = tuple(
            2 ** (self._dz_levels - dz_level - 1) for dz_level in range(self._dz_levels)
        )

        # Preferred slide levels for each Deep Zoom level
        self._slide_from_dz_level = tuple(
            self._osr.get_best_level_for_downsample(d) for d in l0_z_downsamples
        )

        # Piecewise downsamples
        self._l0_l_downsamples = self._osr.level_downsamples
        self._l_z_downsamples = tuple(
            l0_z_downsamples[dz_level]
            / self._l0_l_downsamples[self._slide_from_dz_level[dz_level]]
            for dz_level in range(self._dz_levels)
        )

        # Slide background color
        self._bg_color = '#' + self._osr.properties.get(
            openslide.PROPERTY_NAME_BACKGROUND_COLOR, 'ffffff'
        )
    