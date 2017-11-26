(function () {

    var BOUNDS = new google.maps.LatLngBounds(
        new google.maps.LatLng(35.077915, 139.234516),
        new google.maps.LatLng(35.882374, 140.158054));
    var INITIAL_CENTER = new google.maps.LatLng(35.689893,139.691456);
    var INITIAL_ZOOM = 12;
    var INITIAL_OPACITY = 50;
    var CHACHE_SIZE = 100;
    var geocoder = new google.maps.Geocoder();

    var map;
    var zoomLabel;
    var infoWindow;
    var kml;

    var viewState;

    function debug(msg) {
        console.log(msg);
    }

    function TileLayer() /* implements MapType */ {
        this.tileSize = new google.maps.Size(256, 256);
        this.maxZoom = 16;
        this.minZoom = 10;
        this.name = "TileLayer";
        this.opacity = viewState.opacity;
        this.tiles = Array();
    }

    function getTileName(p, z) {
        return "" + p.x + "-" + p.y + "-" + z;
    }

    function setOpacity(obj, opacity) {
        if (typeof(obj.style.filter) == "string")
            obj.style.filter = "alpha(opacity:"+ opacity + ")";
        var rate = opacity / 100;
        if (typeof(obj.style.KHTMLOpacity) == "string")
            obj.style.KHTMLOpacity = rate;
        if (typeof(obj.style.MozOpacity) == "string")
            obj.style.MozOpacity = rate;
        if (typeof(obj.style.opacity) == "string")
            obj.style.opacity = rate;
    }

    TileLayer.prototype.setOpacity = function(v) {
        this.opacity = v;
        for (var i = 0; i < this.tiles.length; ++i)
            setOpacity(this.tiles[i], v);
    }

    TileLayer.prototype.getTile = function(p, z, doc) {
        var name = getTileName(p, z);
        for (var i = 0; i < this.tiles.length; ++i)
            if (this.tiles[i].id == name)
                return this.tiles[i];
        var tile = doc.createElement("img");
        tile.id = name;
//        tile.className = "layer-image";
        tile.style.width = this.tileSize.width + "px";
        tile.style.height = this.tileSize.height + "px";
        tile.src = "image/theme1/" + z + "/" + name + ".png";
        tile.onerror = function(){
            this.onerror = null;
            this.src= "image/blank.gif";
        }; 
        this.tiles.push(tile);
//        debug("chach size=" + this.tiles.length + " " + name);
        while (this.tiles.length > CHACHE_SIZE)
            this.tiles.shift();
        setOpacity(tile, this.opacity);
        return tile;
    }

//    TileLayer.prototype.releaseTile = function(tile) {
//        debug("release: " + tile.id);
//    }

    function newNode(name, parent) {
        var node = document.createElement(name);
        if (parent) parent.appendChild(node);
        return node;
    }

    function query(text) {
        var req = {
            address: text,
            bounds: BOUNDS
        };
        geocoder.geocode(req,
            function(res, status) {
                if (status != google.maps.GeocoderStatus.OK) return;
                var loc = res[0].geometry.location;
                if (!BOUNDS.contains(loc)) return;
                infoWindow.setContent(res[0].formatted_address +
                    "<br>" + loc);
                infoWindow.setPosition(loc);
                map.panTo(loc);
                infoWindow.open(map);
            }
        );
    }

    function createZoomLabel() {
        var zoomLabel = document.getElementById("zoom-label");
        zoomLabel.innerText = viewState.zoom;
        zoomLabel.parentNode.removeChild(zoomLabel);
        return zoomLabel;
    }

    function createMapControl(layer) {
        var control = document.getElementById("map-control");
        var settingsButton = document.getElementById("settings-button");
        var about = document.getElementById("about");
        var inputDiv = document.getElementById("input-div");
        var slider = document.getElementById("opacity-slider");
        slider.value = viewState.opacity;
        slider.title = "段彩図の透明度を変更します";
        slider.onchange = function (e) {
            debug("changed: " + e.target.value);
            layer.setOpacity(e.target.value);
            viewState.setOpacity(e.target.value);
        };
        var queryText = document.getElementById("query-text");
        queryText.onkeypress = function(e) {
            if (e.keyCode === 13) query(queryText.value);
        };
//        var ac = new google.maps.places.Autocomplete(queryText, {});
        var queryButton = document.getElementById("query-button");
        queryButton.onclick = function () {
            query(queryText.value);
        };
        var small = true;
        function sign(v) {
            return v < 0 ? -1 : (v > 0 ? 1 : 0);
        }
        function step(w, ew, sw) {
            w += sw;
            var d = sign(w - ew);
            return d === 0 || d === sign(sw) ? ew : w;
        }
        function settingsButtonClick() {
            small = !small;
            settingsButton.src = small ? "image/plus.gif" : "image/minus.gif";
            function anim(w, h, ew, eh, sw, sh, m) {
                function an() {
                    control.style.width = w;
                    control.style.height = h;
                    if (w === ew && h === eh) return;
                    w = step(w, ew, sw);
                    h = step(h, eh, sh);
                    setTimeout(an, m);
                }
                setTimeout(an, m);
            };
            var W = 6;  // 幅リサイズのステップ(px)
            var H = 2;  // 高さリサイズのステップ(px)
            var M = 2;  // リサイズ間隔(ms)
            if (small)
                anim(220, 90, 20, 20, -W, -H, M);
            else
                anim(20, 20, 220, 90, W, H, M);
        };
        settingsButton.addEventListener("click", settingsButtonClick);
        control.parentNode.removeChild(control);
        return control;
    }

    function getQueryString() {
        var vars = [];
        var url = window.location.search;
        if (url.length <= 0) return vars;
        var hash  = url.slice(1).split("&");    
        var max = hash.length;
        for (var i = 0; i < max; i++) {
            var array = hash[i].split("=");    //keyと値に分割。
            vars.push(array[0]);    //末尾にクエリ文字列のkeyを挿入。
            vars[array[0]] = array[1];    //先ほど確保したkeyに、値を代入。
        }
        return vars;
    }

    function asInt(str, dflt) {
        if (!str) return dflt;
        var i = parseInt(str, 10);
        if (i.isNaN) return dflt;
        return i;
    }

    function ViewState() {
        var vars = getQueryString();
        this.center = INITIAL_CENTER;
        if (vars["c"]) {
            var c = vars["c"].split(",");
            var lat = parseFloat(c[0]);
            var lng = parseFloat(c[1]);
            if (!lat.isNaN && !lng.isNaN)
                this.center = new google.maps.LatLng(lat, lng);
        }
        this.zoom = asInt(vars["z"], INITIAL_ZOOM);
        this.opacity = asInt(vars["o"], INITIAL_OPACITY);
    }

    ViewState.prototype.changed = function (center) {
        // URL更新
        // debug("view state changed: " + this.toString());
        history.replaceState(null, null, this.toURL());
    }

    ViewState.prototype.setCenter = function (center) {
        if (center === this.center) return;
        this.center = center;
        this.changed();
    }

    ViewState.prototype.setZoom = function (zoom) {
        if (zoom === this.zoom) return;
        this.zoom = zoom;
        this.changed();
    }

    ViewState.prototype.setOpacity = function (opacity) {
        if (opacity === this.opacity) return;
        this.opacity = opacity;
        this.changed();
    }

    ViewState.prototype.toString = function () {
        return "ViewState(center=" + this.center
            + ", zoom=" + this.zoom
            + ", opacity=" + this.opacity
            + ")";
    }

    ViewState.prototype.toURL = function () {
        return "?c=" + this.center.lat() + "," + this.center.lng()
            + "&z=" + this.zoom
            + "&o=" + this.opacity;
    }

    function initialize() {
        viewState = new ViewState();
        debug("" + viewState);
        var mapDiv = document.getElementById("map-canvas");
        var mapOptions = {
            zoom: viewState.zoom,
            center: viewState.center,
            panControl: false,
            mapTypeId: google.maps.MapTypeId.ROADMAP,
            mapTypeControl: false,
//          mapTypeControlOptions: {
//              position: google.maps.ControlPosition.RIGHT_TOP
//          },
            zoomControl: true,
            zoomControlOptions: {
                position: google.maps.ControlPosition.RIGHT_BOTTOM
            },
            streetViewControl: true,
            streetViewControlOptions: {
                position: google.maps.ControlPosition.RIGHT_BOTTOM
            },
            scaleControl: true,
            fullscreenControl: true
        }

        map = new google.maps.Map(mapDiv, mapOptions);
//      if (ContextCompat.checkSelfPermission(
//          this, Manifest.permission.ACCESS_FINE_LOCATION)
//              == PackageManager.PERMISSION_GRANTED) {
//          mMap.setMyLocationEnabled(true);
//      }
        google.maps.event.addListener(map, "zoom_changed", function() {
            var zoom = map.getZoom();
            zoomLabel.innerText = zoom;
            viewState.setZoom(zoom);
        });
//        google.maps.event.addListener(map, "center_changed", function() {
//            debug("center=" + map.getCenter());
//        });
//        google.maps.event.addListener(map, "dragstart", function() {
//            debug("dragstart");
//        });
        google.maps.event.addListener(map, "dragend", function() {
            var center = map.getCenter();
            viewState.setCenter(center);
        });

        var streetDiv = document.getElementById("street-view");
        var streetOpts = {
            scrollwheel: false,
            enableCloseButton: true
        };
        var streetView = new google.maps.StreetViewPanorama(
            streetDiv, streetOpts);
        map.setStreetView(streetView);
        var stv = 9;
        google.maps.event.addListener(streetView, "visible_changed",
            function () {
                if (streetView.getVisible()) {
                    if (stv == 0) {
                        mapDiv.style.width = "50%";
                        stv = 1;
                    }
                    if (stv == 9) {
                        mapDiv.style.width = "100%";
                        stv = 0;
                    }
                } else {
                    if (stv == 1) {
                        mapDiv.style.width = "100%";
                        stv = 0;
                    }
                }
                google.maps.event.trigger(map, "resize");
//                map.setZoom(map.getZoom());
            }
        );
        google.maps.event.addListener(streetView, "position_changed",
            function () {
                if (stv == 1) {
                    var pos = streetView.getPosition();
                    map.panTo(pos);
                }
            }
        );

        var layer = new TileLayer();

        zoomLabel = createZoomLabel();
        map.controls[google.maps.ControlPosition.TOP_LEFT].push(zoomLabel);

        var control = createMapControl(layer);
        map.controls[google.maps.ControlPosition.TOP_LEFT].push(control);

        var authority = document.getElementById("authority");
        authority.parentNode.removeChild(authority);
        map.controls[google.maps.ControlPosition.BOTTOM_RIGHT].push(authority);

        map.overlayMapTypes.insertAt(0, layer);

        infoWindow = new google.maps.InfoWindow({
            position: INITIAL_CENTER,
            content: "こんにちは"
        });

        if (location.search.indexOf("?q=") === 0) {
            var kmlUrl = location.search.substring(3);
            kmlUrl = decodeURIComponent(kmlUrl);
//            kmlUrl += "?t=" + new Date().getTime();
            kml = new google.maps.KmlLayer(kmlUrl);
            kml.setMap(map);
        }

    };

    google.maps.event.addDomListener(window, "load", initialize);

}());
