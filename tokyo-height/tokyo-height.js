(function () {

    const IMAGE_DIR = "image";
    const BOUNDS = new google.maps.LatLngBounds(
        new google.maps.LatLng(35.077915, 139.234516),
        new google.maps.LatLng(35.882374, 140.158054));
    const INITIAL_CENTER = new google.maps.LatLng(35.689893,139.691456);
    const INITIAL_ZOOM = 12;
    const INITIAL_OPACITY = 50;
    const CHACHE_SIZE = 100;
    const geocoder = new google.maps.Geocoder();

    let map;
    let zoomLabel;
    let infoWindow;
    let kml;

    let viewState;

    function debug(msg) {
        console.log(msg);
    }

    function getTileName(p, z) {
        return "" + p.x + "-" + p.y + "-" + z;
    }

    function setOpacity(obj, opacity) {
        if (typeof(obj.style.filter) == "string")
            obj.style.filter = "alpha(opacity:"+ opacity + ")";
        const rate = opacity / 100;
        if (typeof(obj.style.KHTMLOpacity) == "string")
            obj.style.KHTMLOpacity = rate;
        if (typeof(obj.style.MozOpacity) == "string")
            obj.style.MozOpacity = rate;
        if (typeof(obj.style.opacity) == "string")
            obj.style.opacity = rate;
    }


    /**
     * MapTypeインタフェースの実装
     * @see https://developers.google.com/maps/documentation/javascript/maptypes?hl=ja
     */
    class TileLayer /* implements MapType */ {

        constructor() {
            this._tileSize = new google.maps.Size(256, 256);
            this._opacity = viewState.opacity;
            this._tiles = Array();
        }

        get maxZoom() { return 16; }
        get minZoom() { return 10; }
        get tileSize() { return this._tileSize; }

        set opacity(v) {
            this._opacity = v;
            for (let i = 0; i < this._tiles.length; ++i)
                setOpacity(this._tiles[i], v);
        }

        _makeTile(doc, name, z) {
            const tile = doc.createElement("div");
            tile.id = name;
            tile.className = "map-tile";
            const img = doc.createElement("img");
            img.src = IMAGE_DIR + "/theme1/" + z + "/" + name + ".png";
            img.onerror = function(){
                this.onerror = null;
                this.src= IMAGE_DIR + "/blank.gif";
            }; 
            tile.appendChild(img);
            const text = doc.createElement("span");
            if (viewState.label !== 0)
                text.innerText = name;
            text.className = "map-label";
            tile.appendChild(text);
            return tile;
        }

        getTile(p, z, doc) {
            const name = getTileName(p, z);
            for (let i = 0; i < this._tiles.length; ++i)
                if (this._tiles[i].id == name)
                    return this._tiles[i];
            const tile = this._makeTile(doc, name, z);
            this._tiles.push(tile);
            while (this._tiles.length > CHACHE_SIZE)
                this._tiles.shift();
            setOpacity(tile, this._opacity);
            return tile;
        }

    //    releaseTile(tile) {
    //        debug("release: " + tile.id);
    //    }
    }

    function newNode(name, parent) {
        const node = document.createElement(name);
        if (parent) parent.appendChild(node);
        return node;
    }

    function query(text) {
        const req = {
            address: text,
            bounds: BOUNDS
        };
        geocoder.geocode(req,
            function(res, status) {
                if (status != google.maps.GeocoderStatus.OK) return;
                const loc = res[0].geometry.location;
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
        const zoomLabel = document.getElementById("zoom-label");
        zoomLabel.innerText = viewState.zoom;
        zoomLabel.parentNode.removeChild(zoomLabel);
        return zoomLabel;
    }

    function createMapControl(layer) {
        const control = document.getElementById("map-control");
        const settingsButton = document.getElementById("settings-button");
        const about = document.getElementById("about");
        const inputDiv = document.getElementById("input-div");
        const slider = document.getElementById("opacity-slider");
        slider.value = viewState.opacity;
        slider.title = "段彩図の透明度を変更します";
        slider.onchange = function (e) {
//          debug("changed: " + e.target.value);
            layer.opacity = e.target.value;
            viewState.opacity = e.target.value;
        };
        const queryText = document.getElementById("query-text");
        queryText.onkeypress = function(e) {
            if (e.keyCode === 13) query(queryText.value);
        };
//        const ac = new google.maps.places.Autocomplete(queryText, {});
        const queryButton = document.getElementById("query-button");
        queryButton.onclick = function () {
            query(queryText.value);
        };
        let small = true;
        function sign(v) {
            return v < 0 ? -1 : (v > 0 ? 1 : 0);
        }
        function step(w, ew, sw) {
            w += sw;
            const d = sign(w - ew);
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
            const W = 6;  // 幅リサイズのステップ(px)
            const H = 2;  // 高さリサイズのステップ(px)
            const M = 2;  // リサイズ間隔(ms)
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
        const vars = [];
        const url = window.location.search;
        if (url.length <= 0) return vars;
        const hash  = url.slice(1).split("&");    
        const max = hash.length;
        for (let i = 0; i < max; i++) {
            const array = hash[i].split("=");    //keyと値に分割。
            vars.push(array[0]);    //末尾にクエリ文字列のkeyを挿入。
            vars[array[0]] = array[1];    //先ほど確保したkeyに、値を代入。
        }
        return vars;
    }

    function asInt(str, dflt) {
        if (!str) return dflt;
        const i = parseInt(str, 10);
        if (i.isNaN) return dflt;
        return i;
    }

    class ViewState {
        constructor() {
            const vars = getQueryString();
            let center = INITIAL_CENTER;
            if (vars["c"]) {
                const c = vars["c"].split(",");
                const lat = parseFloat(c[0]);
                const lng = parseFloat(c[1]);
                if (!lat.isNaN && !lng.isNaN)
                    center = new google.maps.LatLng(lat, lng);
            }
            this._center = center;
            this._zoom = asInt(vars["z"], INITIAL_ZOOM);
            this._opacity = asInt(vars["o"], INITIAL_OPACITY);
            this._label = asInt(vars["l"], 0);
            if (vars["k"] != null)
                this._kml = decodeURIComponent(vars["k"]);
            else
                this._kml = null;
        }

        changed() {
            // 現在のURLを変更します。
//          debug(this.toURL());
            history.replaceState(null, null, this.toURL());
        }

        get center() { return this._center; }
        set center(value) {
            this._center = value;
            this.changed();
        }

        get zoom() { return this._zoom; }
        set zoom(value) {
            this._zoom = value;
            this.changed();
        }
        
        get opacity() { return this._opacity; }
        set opacity(value) {
            this._opacity = value;
            this.changed();
        }

        get label() { return this._label; }

        get kml() { return this._kml; }
        set kml(value) {
            this._kml = value;
            this.changed();
        }

        toString() {
            return "ViewState(center=" + this._center
                + ", zoom=" + this._zoom
                + ", opacity=" + this._opacity
                + ", label=" + this._label
                + ", kml=" + this._kml
                + ")";
        }

        toURL() {
            return "?c=" + this._center.lat() + "," + this._center.lng()
                + "&z=" + this._zoom
                + "&o=" + this._opacity
                + "&l=" + this._label
                + (this._kml != null ? "&k=" + encodeURIComponent(this._kml) : "");
        }

    }


    function initialize() {
        viewState = new ViewState();
//      debug("" + viewState);
        const mapDiv = document.getElementById("map-canvas");
        const mapOptions = {
            zoom: viewState.zoom,
            center: viewState.center,
            panControl: false,
            mapTypeControl: false,
            zoomControlOptions: {
                position: google.maps.ControlPosition.RIGHT_BOTTOM
            },
            streetViewControl: true,
            streetViewControlOptions: {
                position: google.maps.ControlPosition.RIGHT_BOTTOM
            },
            scaleControl: true,
            mapTypeId: google.maps.MapTypeId.ROADMAP
        }

        map = new google.maps.Map(mapDiv, mapOptions);
        google.maps.event.addListener(map, "zoom_changed", function() {
            const zoom = map.getZoom();
            zoomLabel.innerText = zoom;
            viewState.zoom = zoom;
        });
//        google.maps.event.addListener(map, "center_changed", function() {
//            debug("center=" + map.getCenter());
//        });
//        google.maps.event.addListener(map, "dragstart", function() {
//            debug("dragstart");
//        });
        google.maps.event.addListener(map, "dragend", function() {
            const center = map.getCenter();
            viewState.center = center;
        });

        const streetDiv = document.getElementById("street-view");
        const streetOpts = {
            scrollwheel: false,
            enableCloseButton: true
        };
        const streetView = new google.maps.StreetViewPanorama(streetDiv, streetOpts);
        map.setStreetView(streetView);
        google.maps.event.addListener(streetView, "visible_changed",
            function () {
                mapDiv.style.width = streetView.getVisible() ? "50%" : "100%";
                google.maps.event.trigger(map, "resize");
//                map.setZoom(map.getZoom());
            }
        );
        google.maps.event.addListener(streetView, "position_changed",
            function () {
                const pos = streetView.getPosition();
                map.panTo(pos);
            }
        );
        streetView.setVisible(false);

        const layer = new TileLayer();

        zoomLabel = createZoomLabel();
        map.controls[google.maps.ControlPosition.TOP_LEFT].push(zoomLabel);

        const control = createMapControl(layer);
        map.controls[google.maps.ControlPosition.TOP_LEFT].push(control);

        const authority = document.getElementById("authority");
        authority.parentNode.removeChild(authority);
        map.controls[google.maps.ControlPosition.BOTTOM_RIGHT].push(authority);

        map.overlayMapTypes.insertAt(0, layer);

        infoWindow = new google.maps.InfoWindow({
            position: INITIAL_CENTER,
            content: "こんにちは"
        });

        if (viewState.kml != null) {
            kml = new google.maps.KmlLayer(viewState.kml);
            kml.setMap(map);
        }


    };

    google.maps.event.addDomListener(window, "load", initialize);

}());
