from pathlib import Path
s=(Path(__file__).resolve().parents[1]/'app/src/main/assets/map.html').read_text()
for token in ['rgSetHeadingCamera(headingUp,bearing,speedKmh)','rgHeadingCameraTick',"getPane('tilePane')","getPane('overlayPane')","getPane('markerPane')",'rgHeadingCamera.enabled','map.getSize().y*.23','requestAnimationFrame(rgHeadingCameraTick)','userCameraHoldUntil']:
 assert token in s,token
print('9 heading-camera checks passed')
