# tokyo-height

## 目的
tokyo-heightのサイトにおいて、以下の操作を行った時にURLを更新して履歴に追加する。
表示範囲の変更
ズームレベルの変更
陰影図透過度の変更
更新されたURLでアクセスされた場合、その時点の表示範囲、ズームレベル、陰影図透過度を復元する。

## ページ構成
メイン div id=map-canvas
ストリートビュー div id=street-view
ズームレベル div id=zoom-label
ダイアログ div id=map-control
出典 div=authority
この地図の作成に当たっては、国土地理院長の承認を得て…

## tokyo-height.js
### 全体の構成
名前空間を汚さないように以下のように構成している。

(function () {
    // 本体
}());
### 定数
#### BOUNDS 
new google.maps.LatLng(35.077915, 139.234516)

new google.maps.LatLng(35.882374, 140.158054)


### INITIAL_CENTER
new google.maps.LatLng(35.689893,139.691456)

### TileLayerクラス
MapType インターフェースを実装しているクラスでは、次のプロパティの定義と指定が必要です。

* tileSize（必須）は、（タイプ google.maps.Size の）タイルのサイズを指定します。サイズは、長方形である必要がありますが、正方形である必要はありません。
* maxZoom（必須）は、このマップタイプのタイルを表示する最大ズームレベルを指定します。
* minZoom（オプション）は、このマップタイプのタイルを表示する最小ズームレベルを指定します。デフォルトでは、この値は 0 であり、最小ズームレベルが存在しないことを示しています。
* name（オプション）は、このマップタイプの名前を指定します。このプロパティは、このマップタイプを MapType コントロールで選択可能にする場合のみ必要です（下記の MapType コントロールの追加をご覧ください）。
* alt（オプション）は、ホバーテキストとして表示されるこのマップタイプの代替テキストを指定します。このプロパティは、このマップタイプを MapType コントロールで選択可能にする場合のみ必要です（下記の MapType コントロールの追加をご覧ください）。

また、MapType インターフェースを実装しているクラスは、次のメソッドを実装する必要があります。

* getTile()（必須）は、API が指定のビューポートに新しいタイルを表示する必要があると判断した場合に必ず呼び出されます。
getTile() メソッドは、次のシグネチャを持つ必要があります。
```
    getTile(tileCoord:Point,zoom:number,ownerDocument:Document):Node
```
API は、MapType の tileSize、minZoom、および maxZoom の各プロパティと、マップの現在のビューポートとズームレベルに基づいて、getTile() の呼び出しが必要かどうかを判断します。このメソッドのハンドラは、渡された座標、ズームレベル、およびタイル画像を付加する DOM 要素を受け取り、HTML 要素を返す必要があります。

* releaseTile()（オプション）は、タイルがビューの範囲外になった場合に、マップで該当タイルを削除する必要があると API が判断した場合に必ず呼び出されます。このメソッドは、次のシグネチャを持つ必要があります。
```
    releaseTile(tile:Node)
```
通常、マップに加え、マップタイルに付加したすべての要素の除去を処理する必要がありますたとえば、マップタイル オーバーレイにイベントリスナを付加した場合、これらもここで除去する必要があります。

#### tileSizeプロパティ
#### maxZoomプロパティ(=16)
#### minZoomプロパティ(=10)
#### nameプロパティ(=”TileLayer”)
#### oapacityプロパティ(=INITIAL_OPACITY=50)
#### setOpacity(v)メソッド
#### getTile(p, z, doc)メソッド
### 関数
#### getTileName(p, z)
#### setOpacity(obj, opacity)
#### newNode(name, parent)
#### query(text)
#### createZoomLabel()
#### createMapControl(layer)
#### initialize()
##### map : google.maps.Mapの作成
zoom, center, streetViewControl, scaleControl等mapOptionsを指定する。
オプションは以下のようになっている。

        var mapOptions = {
            zoom: viewState.zoom,
            center: viewState.center,
            panControl: false,
            mapTypeControlOptions: {
                position: google.maps.ControlPosition.RIGHT_TOP
            },
            zoomControlOptions: {
                position: google.maps.ControlPosition.LEFT_CENTER
            },
            streetViewControl: true,
            streetViewControlOptions: {
                position: google.maps.ControlPosition.LEFT_CENTER
            },
            scaleControl: true,
            mapTypeId: google.maps.MapTypeId.ROADMAP
        }
##### map.zoom_changedイベント処理の設定
zoom-labelのテキストとして現在のmapのズーム値を設定する。
##### streetView : google.maps.StreetViewPanoramaの作成
##### streetView.visible_changedイベント処理の設定
##### streetView.position_changedイベント処理の設定
##### layer : TileLayerの作成
### メイン
google.maps.event.addDomListener(window, "load", initialize);
## 関連するGoogleMaps API
### イベント
以下のドキュメントに記述がある。
Google Maps API イベント
#### center_changed
地図をドラッグすると大量に発生する。このイベントが発生する都度URLおよび履歴を更新すると履歴が膨大な量になる。そのためdragendイベントを拾った方が良いと思われる。dragendイベント発生時にcenterプロパティを取得してURLおよび履歴を更新する。
#### zoom_changed
div id=zoom-labelを更新するために既にフックしている。このときにURLおよび履歴を更新する。
#### slider.onchange
透過度を変更するスライダーに変更があったときにURLおよび履歴を更新する。


