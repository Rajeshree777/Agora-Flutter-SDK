/**/import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'dart:math' as math;

import 'package:soda_app/utils/print_log.dart';

@immutable
class FilterSelector extends StatefulWidget {
  const FilterSelector({
    Key key,
    @required this.filters,
    @required this.onFilterChanged,
    this.padding = const EdgeInsets.only(top: 8.0, bottom: 8.0),
  }) : super(key: key);

  final List<String> filters;
  final void Function(String effectName) onFilterChanged;
  final EdgeInsets padding;

  @override
  _FilterSelectorState createState() => _FilterSelectorState();
}

class _FilterSelectorState extends State<FilterSelector> {
  static const _filtersPerScreen = 5;
  static const _viewportFractionPerItem = 1.0 / _filtersPerScreen;

  PageController _controller;
  int _page;
  bool isScrollEnd = false;

  int get filterCount => widget.filters.length;

  String itemColor(int index) => widget.filters[index % filterCount];

  @override
  void initState() {
    super.initState();
    _page = 0;
    _controller = PageController(
      initialPage: _page,
      viewportFraction: _viewportFractionPerItem,
    );
    _controller.addListener(_onPageChanged);
  }

  void _onPageChanged() {
    final page = (_controller.page ?? 0).round();
    if (page != _page) {
      _page = page;
    }
  }

  void _onFilterTapped(int index) {
    _controller.animateToPage(
      index,
      duration: const Duration(milliseconds: 450),
      curve: Curves.ease,
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      child: NotificationListener(
        onNotification: (t) {
          if (t is ScrollEndNotification) {
            print("animate to page : ${_controller.page.round()}");
            widget.onFilterChanged(widget.filters[_controller.page.round()]);
          }
          return;
        },
        child: Scrollable(
          controller: _controller,
          axisDirection: AxisDirection.right,
          physics: PageScrollPhysics(),
          viewportBuilder: (context, viewportOffset) {
            return LayoutBuilder(
              builder: (context, constraints) {
                final itemSize = (constraints.maxWidth * _viewportFractionPerItem);
                viewportOffset
                  ..applyViewportDimension(constraints.maxWidth)
                  ..applyContentDimensions(0.0, (itemSize) * (filterCount - 1));

                return Stack(
                  alignment: Alignment.bottomCenter,
                  children: [
                    //_buildShadowGradient(itemSize),
                    _buildCarousel(
                      viewportOffset: viewportOffset,
                      itemSize: itemSize,
                    ),
                    _buildSelectionRing(itemSize),
                  ],
                );
              },
            );
          },
        ),
      ),
    );
  }

  Widget _buildCarousel({
    @required ViewportOffset viewportOffset,
    @required double itemSize,
  }) {
    return Container(
      height: itemSize,
      margin: widget.padding,
      child: Flow(
        delegate: CarouselFlowDelegate(
          viewportOffset: viewportOffset,
          filtersPerScreen: _filtersPerScreen,
        ),
        children: [
          for (int i = 0; i < filterCount; i++)
            FilterItem(
              onEffectSelected: () => _onFilterTapped(i),
              effectName: itemColor(i),
            ),
        ],
      ),
    );
  }

  Widget _buildSelectionRing(double itemSize) {
    return IgnorePointer(
      child: Padding(
        padding: widget.padding,
        child: SizedBox(
          width: itemSize,
          height: itemSize,
          child: const DecoratedBox(
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              border: Border.fromBorderSide(
                BorderSide(width: 3.0, color: Colors.white),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class CarouselFlowDelegate extends FlowDelegate {
  CarouselFlowDelegate({
    @required this.viewportOffset,
    @required this.filtersPerScreen,
  }) : super(repaint: viewportOffset);

  final ViewportOffset viewportOffset;
  final int filtersPerScreen;

  @override
  void paintChildren(FlowPaintingContext context) {
    final count = context.childCount;

    // All available painting width
    final size = context.size.width;

    // The distance that a single item "page" takes up from the perspective
    // of the scroll paging system. We also use this size for the width and
    // height of a single item.
    final itemExtent = size / filtersPerScreen;
    // The current scroll position expressed as an item fraction, e.g., 0.0,
    // or 1.0, or 1.3, or 2.9, etc. A value of 1.3 indicates that item at
    // index 1 is active, and the user has scrolled 30% towards the item at
    // index 2.
    final active = viewportOffset.pixels / itemExtent;
    // Index of the first item we need to paint at this moment.
    // At most, we paint 3 items to the left of the active item.
    final min = math.max(0, active.floor() - 3).toInt();
    // Index of the last item we need to paint at this moment.
    // At most, we paint 3 items to the right of the active item.
    final max = math.min(count - 1, active.ceil() + 3).toInt();
    // Generate transforms for the visible items and sort by distance.
    for (var index = min; index <= max; index++) {
      final itemXFromCenter = itemExtent * index - viewportOffset.pixels;
      final percentFromCenter = 1.0 - (itemXFromCenter / (size / 2)).abs();
      final itemScale = 0.5 + (percentFromCenter * 0.5);
      final opacity = 0.25 + (percentFromCenter * 0.75);
      final itemTransform = Matrix4.identity()
        ..translate((size - itemExtent) / 2)
        ..translate(itemXFromCenter)
        ..translate(itemExtent / 2, itemExtent / 2)
        ..multiply(Matrix4.diagonal3Values((index == active.round())? itemScale : 0.7, (index == active.round())? itemScale : 0.7, 1))
        ..translate(-itemExtent / 2, -itemExtent / 2);

      context.paintChild(
        index,
        transform: itemTransform,
        // opacity: opacity,
      );
    }
  }

  @override
  bool shouldRepaint(covariant CarouselFlowDelegate oldDelegate) {
    return oldDelegate.viewportOffset != viewportOffset;
  }
}

@immutable
class FilterItem extends StatelessWidget {
  FilterItem({
    Key key,
    @required this.effectName,
    this.onEffectSelected,
  }) : super(key: key);

  final String effectName;
  final VoidCallback onEffectSelected;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onEffectSelected,
      child: AspectRatio(
        aspectRatio: 1.0,
        child: Padding(
          padding: const EdgeInsets.all(2.0),
          child: ClipOval(
            child: Image.network(
              'https://flutter.dev/docs/cookbook/img-files/effects/instagram-buttons/millenial-texture.jpg',
            ),
          ),
        ),
      ),
    );
  }
}
