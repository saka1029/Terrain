# Problems



## APIキーがない （解決済）

> util.js:246 Google Maps API warning:
> NoApiKeys https://developers.google.com/maps/documentation/javascript/error-messages#no-api-keys
    
index.htmlでは以下のscriptタグでGoogle Maps APIライブラリを読み込んでいる。

    <script src="https://maps.googleapis.com/maps/api/js?sensor=false&amp;language=ja&amp;libraries=places"></script>

サイト https://developers.google.com/maps/web/ で「キーの取得」ボタンを押して以下のAPIキーを得た。

 項目 | 値 
-------|----
 プロジェクト | Terrain 
 API キー     | AIzaSyCf8VEH-Ojgr_XqudkPWkuCJoXFpsBMkrs 
 
 インクルードするAPIライブラリのパラメータに「key=AIzaSyCf8VEH-Ojgr_XqudkPWkuCJoXFpsBMkrs」を追加する。

## Deprecation （解決済）

> [Deprecation] The deviceorientation event is deprecated on insecure origins,
> and support will be removed in the future.
> You should consider switching your application to a secure origin,
> such as HTTPS. See https://goo.gl/rStTGz for more details.

Google maps APIに端末の向きを検知するdeviceorentationイベントがあるらしいが、
HTTPSでないサイトの場合は無効になるようである。

## SensorNotRequired （解決済）

> Google Maps API warning: SensorNotRequired https://developers.google.com/maps/documentation/javascript/error-messages#sensor-not-required

index.htmlでは以下のscriptタグでGoogle Maps APIライブラリを読み込んでいる。

    <script src="https://maps.googleapis.com/maps/api/js?sensor=false&amp;language=ja&amp;libraries=places"></script>

一方Googleの公式サイトでは以下の記述がある。

> The sensor parameter is no longer required for the Google Maps JavaScript API.
> It won’t prevent the Google Maps JavaScript API from working correctly,
> but we recommend that you remove the sensor parameter from the script element.

インクルードするAPIのURLから「sensor=false」を削除する。