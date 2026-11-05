import math, sys
def bez(p0,p1,p2,p3,t):
    return tuple((1-t)**3*a+3*(1-t)**2*t*b+3*(1-t)*t**2*c+t**3*d for a,b,c,d in zip(p0,p1,p2,p3))
def icon(accent1, accent2):
    P0,P1,P2,P3=(20,90),(28,68),(42,52),(64,45)
    road = "M%.1f,%.1f C%.1f,%.1f %.1f,%.1f %.1f,%.1f"%(*P0,*P1,*P2,*P3)
    edge = "M17,86 C25,64 41,49 66,41"
    dashes=[]
    n=9
    for i in range(n):
        t0=0.06+i/n*0.9; t1=t0+0.045
        a=bez(P0,P1,P2,P3,t0); b=bez(P0,P1,P2,P3,t1)
        dashes.append("M%.2f,%.2f L%.2f,%.2f"%(*a,*b))
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Logo Pilote : P argenté qui devient une route, flèche de navigation. Généré par tools/icon_gen.py. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android" xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:pathData="{edge}" android:strokeWidth="2.6" android:strokeLineCap="round" android:fillColor="#00000000">
        <aapt:attr name="android:strokeColor"><gradient android:type="linear" android:startX="17" android:startY="86" android:endX="66" android:endY="41">
            <item android:offset="0" android:color="#00{accent1[1:]}"/><item android:offset="0.5" android:color="#FF{accent1[1:]}"/><item android:offset="1" android:color="#FF{accent2[1:]}"/></gradient></aapt:attr>
    </path>
    <path android:pathData="{road}" android:strokeColor="#FF0A0E15" android:strokeWidth="9" android:strokeLineCap="round" android:fillColor="#00000000"/>
    <path android:pathData="{' '.join(dashes)}" android:strokeColor="#FFFFFFFF" android:strokeWidth="1.5" android:strokeLineCap="butt" android:fillColor="#00000000"/>
    <path android:fillType="evenOdd" android:pathData="M42,80 L47,27 L68,27 C80,27 86,35 84,45 C82,56 73,62 62,62 L52,62 L50,80 Z M54,37 L52.6,52 L61,52 C67,52 71,49 72,45 C72.6,40.5 69.5,37 64,37 Z">
        <aapt:attr name="android:fillColor"><gradient android:type="linear" android:startX="44" android:startY="27" android:endX="80" android:endY="80">
            <item android:offset="0" android:color="#FFFFFFFF"/><item android:offset="0.55" android:color="#FFD5DBE3"/><item android:offset="1" android:color="#FF8C96A4"/></gradient></aapt:attr>
    </path>
    <path android:pathData="M61,30 L72,55 L62.5,49.5 L53.5,56 Z" android:strokeColor="#FFFFFFFF" android:strokeWidth="1.2" android:strokeLineJoin="round">
        <aapt:attr name="android:fillColor"><gradient android:type="linear" android:startX="61" android:startY="30" android:endX="62" android:endY="56">
            <item android:offset="0" android:color="#FF{accent2[1:]}"/><item android:offset="1" android:color="#FF{accent1[1:]}"/></gradient></aapt:attr>
    </path>
</vector>
'''
bg = '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:pathData="M0,0 H108 V108 H0 Z">
        <aapt:attr name="android:fillColor"><gradient android:type="linear" android:startX="0" android:startY="0" android:endX="108" android:endY="108">
            <item android:offset="0" android:color="#FF20262F"/><item android:offset="1" android:color="#FF07090D"/></gradient></aapt:attr>
    </path>
</vector>
'''
open(sys.argv[1],"w").write(icon("#0A6CFF","#39B8FF"))
open(sys.argv[2],"w").write(icon("#C4141E","#FF3B3B"))
open(sys.argv[3],"w").write(bg)
