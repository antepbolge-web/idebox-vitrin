#!/usr/bin/env python3
"""ideabox Vitrin'i cm deposundaki surum.json ile eşitler.

Yalnızca kartların `surum`, `tarih` ve `yenilik` alanlarına dokunur; sayfanın geri kalanı değişmez.
Kartlar paket adıyla (yoksa adla) eşleşir. Vitrinde olmayan yeni uygulamalar ve ad değişiklikleri
eklenmez, yalnızca raporlanır (ad değişikliği sayaç taşıma gerektirir: index.html ESKI_ADLAR).
Değişiklik olursa sw.js önbellek sürümü bir artırılır.

Kullanım: python3 tools/esitle.py [surum.json yolu]   (yol verilmezse GitHub'dan indirilir)
Çıkış: değişiklik varsa 0 ve "DEGISTI" yazar; yoksa 0 ve "DEGISMEDI".
"""
import datetime
import json
import os
import re
import sys
import urllib.request

KOK = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SAYFA = os.path.join(KOK, "index.html")
SW = os.path.join(KOK, "sw.js")
ADRES = "https://raw.githubusercontent.com/antepbolge-web/cm/main/surum.json"
KART = re.compile(r'\{ ad: "((?:[^"\\]|\\.)*)",(?P<govde>.*?)aciklama: "(?:[^"\\]|\\.)*", yenilik: "(?P<yenilik>(?:[^"\\]|\\.)*)" \}', re.S)


def js(metin):
    return metin.replace("\\", "\\\\").replace('"', '\\"').replace("\n", " ")


def jsdan(metin):
    return re.sub(r'\\(.)', r'\1', metin)


def alan(govde, ad):
    m = re.search(ad + r': "([^"]*)"', govde)
    return m.group(1) if m else None


def bugun():
    # Türkiye saati (UTC+3, yaz saati yok)
    return (datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(hours=3)).strftime("%Y-%m-%d")


def main():
    if len(sys.argv) > 1:
        kaynak = json.load(open(sys.argv[1], encoding="utf-8"))
    else:
        with urllib.request.urlopen(ADRES, timeout=30) as yanit:
            kaynak = json.loads(yanit.read().decode("utf-8"))
    kayitlar = {k: v for k, v in kaynak.items() if isinstance(v, dict) and not k.startswith("_")}
    paketle = {v["paket"]: v for v in kayitlar.values() if v.get("paket")}
    telefon_adla = {v.get("ad"): v for k, v in kayitlar.items() if v.get("ad") and not k.startswith("pc_")}
    pc_adla = {v.get("ad"): v for k, v in kayitlar.items() if v.get("ad") and k.startswith("pc_")}

    sayfa = open(SAYFA, encoding="utf-8").read()
    bas = sayfa.index("  const UYGULAMALAR = [")
    son = sayfa.index("\n  ];", bas)
    liste = sayfa[bas:son]
    gorulen, rapor, degisen = set(), [], 0

    def degistir(m):
        nonlocal degisen
        ad, govde, eski_not = m.group(1), m.group("govde"), m.group("yenilik")
        paket, yer = alan(govde, "paket"), alan(govde, "yer") or ""
        if paket and paket in paketle:
            kayit = paketle[paket]
        elif yer == "Bilgisayar":
            kayit = pc_adla.get(jsdan(ad))
        else:
            kayit = telefon_adla.get(jsdan(ad))
        if not kayit and paket:
            # surum.json'da paketi boş kayıtlar (ör. "kilit"): anahtar, paket adının son parçasıdır
            kayit = kayitlar.get(paket.rsplit(".", 1)[-1])
        if not kayit:
            return m.group(0)
        gorulen.add(id(kayit))
        if kayit.get("ad") and kayit["ad"] != jsdan(ad):
            rapor.append(f"Ad değişmiş (elle taşınmalı): {jsdan(ad)} -> {kayit['ad']}")
        yeni_surum = re.sub(r"-telefon\.\d+$", "", str(kayit.get("surum", "")))
        eski_surum = alan(govde, "surum")
        yeni_not = js(str(kayit.get("yenilik", "")))
        if not yeni_surum or (yeni_surum == eski_surum and yeni_not == eski_not):
            return m.group(0)
        if yeni_surum != eski_surum:
            govde = govde.replace(f'surum: "{eski_surum}"', f'surum: "{yeni_surum}"', 1)
            govde = re.sub(r'tarih: "[^"]*"', f'tarih: "{bugun()}"', govde, count=1)
            rapor.append(f"{jsdan(ad)}: {eski_surum} -> {yeni_surum}")
        elif yeni_not != eski_not:
            rapor.append(f"{jsdan(ad)}: not güncellendi")
        degisen += 1
        return m.group(0)[: m.start("govde") - m.start(0)] + govde + m.group(0)[m.end("govde") - m.start(0):].replace(
            f'yenilik: "{eski_not}"', f'yenilik: "{yeni_not}"', 1)

    yeni_liste = KART.sub(degistir, liste)
    for k, v in kayitlar.items():
        if id(v) not in gorulen and not k.startswith("pc_"):
            rapor.append(f"Vitrinde yok (elle eklenir): {v.get('ad') or k} {v.get('surum', '')}")

    for satir in rapor:
        print(satir)
    if not degisen:
        print("DEGISMEDI")
        return
    open(SAYFA, "w", encoding="utf-8").write(sayfa[:bas] + yeni_liste + sayfa[son:])
    sw = open(SW, encoding="utf-8").read()
    sw = re.sub(r'const SAKLA = "vitrin-v(\d+)";', lambda s: f'const SAKLA = "vitrin-v{int(s.group(1)) + 1}";', sw, count=1)
    open(SW, "w", encoding="utf-8").write(sw)
    print(f"DEGISTI ({degisen} kart)")


if __name__ == "__main__":
    main()
