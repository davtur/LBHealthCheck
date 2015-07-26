/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


(function (a) {
    a.Jcrop = function (d, D) {
        var L = a.extend({}, a.Jcrop.defaults), ai, au, ak = false;
        function m(av) {
            return parseInt(av, 10) + "px"
        }
        function O(av) {
            return parseInt(av, 10) + "%"
        }
        function F(av) {
            return L.baseClass + "-" + av
        }
        function G() {
            return a.fx.step.hasOwnProperty("backgroundColor")
        }
        function H(av) {
            var aw = a(av).offset();
            return[aw.left, aw.top]
        }
        function J(av) {
            return[(av.pageX - ai[0]), (av.pageY - ai[1])]
        }
        function C(av) {
            if (typeof (av) !== "object") {
                av = {}
            }
            L = a.extend(L, av);
            if (typeof (L.onChange) !== "function") {
                L.onChange = function () {
                }
            }
            if (typeof (L.onSelect) !== "function") {
                L.onSelect = function () {
                }
            }
            if (typeof (L.onRelease) !== "function") {
                L.onRelease = function () {
                }
            }
        }
        function I(av) {
            if (av !== au) {
                T.setCursor(av);
                au = av
            }
        }
        function f(ax, az) {
            ai = H(ar);
            T.setCursor(ax === "move" ? ax : ax + "-resize");
            if (ax === "move") {
                return T.activateHandlers(V(az), q)
            }
            var av = ae.getFixed();
            var aw = s(ax);
            var ay = ae.getCorner(s(aw));
            ae.setPressed(ae.getCorner(aw));
            ae.setCurrent(ay);
            T.activateHandlers(K(ax, av), q)
        }
        function K(aw, av) {
            return function (ax) {
                if (!L.aspectRatio) {
                    switch (aw) {
                        case"e":
                            ax[1] = av.y2;
                            break;
                        case"w":
                            ax[1] = av.y2;
                            break;
                        case"n":
                            ax[0] = av.x2;
                            break;
                        case"s":
                            ax[0] = av.x2;
                            break
                        }
                } else {
                    switch (aw) {
                        case"e":
                            ax[1] = av.y + 1;
                            break;
                        case"w":
                            ax[1] = av.y + 1;
                            break;
                        case"n":
                            ax[0] = av.x + 1;
                            break;
                        case"s":
                            ax[0] = av.x + 1;
                            break
                        }
                }
                ae.setCurrent(ax);
                aa.update()
            }
        }
        function V(aw) {
            var av = aw;
            aq.watchKeys();
            return function (ax) {
                ae.moveOffset([ax[0] - av[0], ax[1] - av[1]]);
                av = ax;
                aa.update()
            }
        }
        function s(av) {
            switch (av) {
                case"n":
                    return"sw";
                case"s":
                    return"nw";
                case"e":
                    return"nw";
                case"w":
                    return"ne";
                case"ne":
                    return"sw";
                case"nw":
                    return"se";
                case"se":
                    return"nw";
                case"sw":
                    return"ne"
                }
        }
        function c(av) {
            return function (aw) {
                if (L.disabled) {
                    return false
                }
                if ((av === "move") && !L.allowMove) {
                    return false
                }
                r = true;
                f(av, J(aw));
                aw.stopPropagation();
                aw.preventDefault();
                return false
            }
        }
        function Y(az, aw, ay) {
            var av = az.width(), ax = az.height();
            if ((av > aw) && aw > 0) {
                av = aw;
                ax = (aw / az.width()) * az.height()
            }
            if ((ax > ay) && ay > 0) {
                ax = ay;
                av = (ay / az.height()) * az.width()
            }
            Q = az.width() / av;
            e = az.height() / ax;
            az.width(av).height(ax)
        }
        function ac(av) {
            return{x: parseInt(av.x * Q, 10), y: parseInt(av.y * e, 10), x2: parseInt(av.x2 * Q, 10), y2: parseInt(av.y2 * e, 10), w: parseInt(av.w * Q, 10), h: parseInt(av.h * e, 10)}
        }
        function q(aw) {
            var av = ae.getFixed();
            if ((av.w > L.minSelect[0]) && (av.h > L.minSelect[1])) {
                aa.enableHandles();
                aa.done()
            } else {
                aa.release()
            }
            T.setCursor(L.allowSelect ? "crosshair" : "default")
        }
        function ah(av) {
            if (L.disabled) {
                return false
            }
            if (!L.allowSelect) {
                return false
            }
            r = true;
            ai = H(ar);
            aa.disableHandles();
            I("crosshair");
            var aw = J(av);
            ae.setPressed(aw);
            aa.update();
            T.activateHandlers(ap, q);
            aq.watchKeys();
            av.stopPropagation();
            av.preventDefault();
            return false
        }
        function ap(av) {
            ae.setCurrent(av);
            aa.update()
        }
        function aj() {
            var av = a("<div></div>").addClass(F("tracker"));
            if (a.browser.msie) {
                av.css({opacity: 0, backgroundColor: "white"})
            }
            return av
        }
        if (a.browser.msie && (a.browser.version.split(".")[0] === "6")) {
            ak = true
        }
        if (typeof (d) !== "object") {
            d = a(d)[0]
        }
        if (typeof (D) !== "object") {
            D = {}
        }
        C(D);
        var i = {border: "none", margin: 0, padding: 0, position: "absolute"};
        var ab = a(d);
        var ar = ab.clone().removeAttr("id").css(i);
        ar.width(ab.width());
        ar.height(ab.height());
        ab.after(ar).hide();
        Y(ar, L.boxWidth, L.boxHeight);
        var U = ar.width(), S = ar.height(), ad = a("<div />").width(U).height(S).addClass(F("holder")).css({position: "relative", backgroundColor: L.bgColor}).insertAfter(ab).append(ar);
        delete (L.bgColor);
        if (L.addClass) {
            ad.addClass(L.addClass)
        }
        var M = a("<img />").attr("src", ar.attr("src")).css(i).width(U).height(S), l = a("<div />").width(O(100)).height(O(100)).css({zIndex: 310, position: "absolute", overflow: "hidden"}).append(M), P = a("<div />").width(O(100)).height(O(100)).css("zIndex", 320), B = a("<div />").css({position: "absolute", zIndex: 300}).insertBefore(ar).append(l, P);
        if (ak) {
            B.css({overflowY: "hidden"})
        }
        var u = L.boundary;
        var b = aj().width(U + (u * 2)).height(S + (u * 2)).css({position: "absolute", top: m(-u), left: m(-u), zIndex: 290}).mousedown(ah);
        var af = L.bgOpacity, A, an, p, X, Q, e, o = true, r, E, ag;
        ai = H(ar);
        var W = (function () {
            function av() {
                var aC = {}, aA = ["touchstart", "touchmove", "touchend"], aB = document.createElement("div"), az;
                try {
                    for (az = 0; az < aA.length; az++) {
                        var ax = aA[az];
                        ax = "on" + ax;
                        var ay = (ax in aB);
                        if (!ay) {
                            aB.setAttribute(ax, "return;");
                            ay = typeof aB[ax] == "function"
                        }
                        aC[aA[az]] = ay
                    }
                    return aC.touchstart && aC.touchend && aC.touchmove
                } catch (aD) {
                    return false
                }
            }
            function aw() {
                if ((L.touchSupport === true) || (L.touchSupport === false)) {
                    return L.touchSupport
                } else {
                    return av()
                }
            }
            return{createDragger: function (ax) {
                    return function (ay) {
                        ay.pageX = ay.originalEvent.changedTouches[0].pageX;
                        ay.pageY = ay.originalEvent.changedTouches[0].pageY;
                        if (L.disabled) {
                            return false
                        }
                        if ((ax === "move") && !L.allowMove) {
                            return false
                        }
                        r = true;
                        f(ax, J(ay));
                        ay.stopPropagation();
                        ay.preventDefault();
                        return false
                    }
                }, newSelection: function (ax) {
                    ax.pageX = ax.originalEvent.changedTouches[0].pageX;
                    ax.pageY = ax.originalEvent.changedTouches[0].pageY;
                    return ah(ax)
                }, isSupported: av, support: aw()}
        }());
        var ae = (function () {
            var ax = 0, aI = 0, aw = 0, aH = 0, aA, ay;
            function aC(aL) {
                aL = az(aL);
                aw = ax = aL[0];
                aH = aI = aL[1]
            }
            function aB(aL) {
                aL = az(aL);
                aA = aL[0] - aw;
                ay = aL[1] - aH;
                aw = aL[0];
                aH = aL[1]
            }
            function aK() {
                return[aA, ay]
            }
            function av(aN) {
                var aM = aN[0], aL = aN[1];
                if (0 > ax + aM) {
                    aM -= aM + ax
                }
                if (0 > aI + aL) {
                    aL -= aL + aI
                }
                if (S < aH + aL) {
                    aL += S - (aH + aL)
                }
                if (U < aw + aM) {
                    aM += U - (aw + aM)
                }
                ax += aM;
                aw += aM;
                aI += aL;
                aH += aL
            }
            function aD(aL) {
                var aM = aJ();
                switch (aL) {
                    case"ne":
                        return[aM.x2, aM.y];
                    case"nw":
                        return[aM.x, aM.y];
                    case"se":
                        return[aM.x2, aM.y2];
                    case"sw":
                        return[aM.x, aM.y2]
                    }
            }
            function aJ() {
                if (!L.aspectRatio) {
                    return aG()
                }
                var aN = L.aspectRatio, aT = L.minSize[0] / Q, aM = L.maxSize[0] / Q, aV = L.maxSize[1] / e, aO = aw - ax, aU = aH - aI, aP = Math.abs(aO), aQ = Math.abs(aU), aR = aP / aQ, aL, aS;
                if (aM === 0) {
                    aM = U * 10
                }
                if (aV === 0) {
                    aV = S * 10
                }
                if (aR < aN) {
                    aS = aH;
                    w = aQ * aN;
                    aL = aO < 0 ? ax - w : w + ax;
                    if (aL < 0) {
                        aL = 0;
                        h = Math.abs((aL - ax) / aN);
                        aS = aU < 0 ? aI - h : h + aI
                    } else {
                        if (aL > U) {
                            aL = U;
                            h = Math.abs((aL - ax) / aN);
                            aS = aU < 0 ? aI - h : h + aI
                        }
                    }
                } else {
                    aL = aw;
                    h = aP / aN;
                    aS = aU < 0 ? aI - h : aI + h;
                    if (aS < 0) {
                        aS = 0;
                        w = Math.abs((aS - aI) * aN);
                        aL = aO < 0 ? ax - w : w + ax
                    } else {
                        if (aS > S) {
                            aS = S;
                            w = Math.abs(aS - aI) * aN;
                            aL = aO < 0 ? ax - w : w + ax
                        }
                    }
                }
                if (aL > ax) {
                    if (aL - ax < aT) {
                        aL = ax + aT
                    } else {
                        if (aL - ax > aM) {
                            aL = ax + aM
                        }
                    }
                    if (aS > aI) {
                        aS = aI + (aL - ax) / aN
                    } else {
                        aS = aI - (aL - ax) / aN
                    }
                } else {
                    if (aL < ax) {
                        if (ax - aL < aT) {
                            aL = ax - aT
                        } else {
                            if (ax - aL > aM) {
                                aL = ax - aM
                            }
                        }
                        if (aS > aI) {
                            aS = aI + (ax - aL) / aN
                        } else {
                            aS = aI - (ax - aL) / aN
                        }
                    }
                }
                if (aL < 0) {
                    ax -= aL;
                    aL = 0
                } else {
                    if (aL > U) {
                        ax -= aL - U;
                        aL = U
                    }
                }
                if (aS < 0) {
                    aI -= aS;
                    aS = 0
                } else {
                    if (aS > S) {
                        aI -= aS - S;
                        aS = S
                    }
                }
                return aF(aE(ax, aI, aL, aS))
            }
            function az(aL) {
                if (aL[0] < 0) {
                    aL[0] = 0
                }
                if (aL[1] < 0) {
                    aL[1] = 0
                }
                if (aL[0] > U) {
                    aL[0] = U
                }
                if (aL[1] > S) {
                    aL[1] = S
                }
                return[aL[0], aL[1]]
            }
            function aE(aO, aQ, aN, aP) {
                var aS = aO, aR = aN, aM = aQ, aL = aP;
                if (aN < aO) {
                    aS = aN;
                    aR = aO
                }
                if (aP < aQ) {
                    aM = aP;
                    aL = aQ
                }
                return[Math.round(aS), Math.round(aM), Math.round(aR), Math.round(aL)]
            }
            function aG() {
                var aM = aw - ax, aL = aH - aI, aN;
                if (A && (Math.abs(aM) > A)) {
                    aw = (aM > 0) ? (ax + A) : (ax - A)
                }
                if (an && (Math.abs(aL) > an)) {
                    aH = (aL > 0) ? (aI + an) : (aI - an)
                }
                if (X / e && (Math.abs(aL) < X / e)) {
                    aH = (aL > 0) ? (aI + X / e) : (aI - X / e)
                }
                if (p / Q && (Math.abs(aM) < p / Q)) {
                    aw = (aM > 0) ? (ax + p / Q) : (ax - p / Q)
                }
                if (ax < 0) {
                    aw -= ax;
                    ax -= ax
                }
                if (aI < 0) {
                    aH -= aI;
                    aI -= aI
                }
                if (aw < 0) {
                    ax -= aw;
                    aw -= aw
                }
                if (aH < 0) {
                    aI -= aH;
                    aH -= aH
                }
                if (aw > U) {
                    aN = aw - U;
                    ax -= aN;
                    aw -= aN
                }
                if (aH > S) {
                    aN = aH - S;
                    aI -= aN;
                    aH -= aN
                }
                if (ax > U) {
                    aN = ax - S;
                    aH -= aN;
                    aI -= aN
                }
                if (aI > S) {
                    aN = aI - S;
                    aH -= aN;
                    aI -= aN
                }
                return aF(aE(ax, aI, aw, aH))
            }
            function aF(aL) {
                return{x: aL[0], y: aL[1], x2: aL[2], y2: aL[3], w: aL[2] - aL[0], h: aL[3] - aL[1]}
            }
            return{flipCoords: aE, setPressed: aC, setCurrent: aB, getOffset: aK, moveOffset: av, getCorner: aD, getFixed: aJ}
        }());
        var aa = (function () {
            var aF, aN = 370;
            var aA = {};
            var aQ = {};
            var ax = false;
            var aE = L.handleOffset;
            function aB(aU) {
                var aV = a("<div />").css({position: "absolute", opacity: L.borderOpacity}).addClass(F(aU));
                l.append(aV);
                return aV
            }
            function aw(aU, aV) {
                var aW = a("<div />").mousedown(c(aU)).css({cursor: aU + "-resize", position: "absolute", zIndex: aV});
                if (W.support) {
                    aW.bind("touchstart", W.createDragger(aU))
                }
                P.append(aW);
                return aW
            }
            function aG(aU) {
                return aw(aU, aN++).css({top: m(-aE + 1), left: m(-aE + 1), opacity: L.handleOpacity}).addClass(F("handle"))
            }
            function aM(aW) {
                var aZ = L.handleSize, aY = aZ, aV = aZ, aX = aE, aU = aE;
                switch (aW) {
                    case"n":
                    case"s":
                        aV = O(100);
                        break;
                    case"e":
                    case"w":
                        aY = O(100);
                        break
                }
                return aw(aW, aN++).width(aV).height(aY).css({top: m(-aX + 1), left: m(-aU + 1)})
            }
            function aI(aU) {
                var aV;
                for (aV = 0; aV < aU.length; aV++) {
                    aQ[aU[aV]] = aG(aU[aV])
                }
            }
            function aK(aY) {
                var aV = Math.round((aY.h / 2) - aE), aU = Math.round((aY.w / 2) - aE), aW = -aE + 1, a2 = -aE + 1, aZ = aY.w - aE, aX = aY.h - aE, a1, a0;
                if (aQ.e) {
                    aQ.e.css({top: m(aV), left: m(aZ)});
                    aQ.w.css({top: m(aV)});
                    aQ.s.css({top: m(aX), left: m(aU)});
                    aQ.n.css({left: m(aU)})
                }
                if (aQ.ne) {
                    aQ.ne.css({left: m(aZ)});
                    aQ.se.css({top: m(aX), left: m(aZ)});
                    aQ.sw.css({top: m(aX)})
                }
                if (aQ.b) {
                    aQ.b.css({top: m(aX)});
                    aQ.r.css({left: m(aZ)})
                }
            }
            function aD(aU, aV) {
                M.css({top: m(-aV), left: m(-aU)});
                B.css({top: m(aV), left: m(aU)})
            }
            function aT(aU, aV) {
                B.width(aU).height(aV)
            }
            function ay() {
                var aU = ae.getFixed();
                ae.setPressed([aU.x, aU.y]);
                ae.setCurrent([aU.x2, aU.y2]);
                aR()
            }
            function aR() {
                if (aF) {
                    return aC()
                }
            }
            function aC() {
                var aU = ae.getFixed();
                aT(aU.w, aU.h);
                aD(aU.x, aU.y);
                if (ax) {
                    aK(aU)
                }
                if (!aF) {
                    aS()
                }
                L.onChange.call(g, ac(aU))
            }
            function aS() {
                B.show();
                if (L.bgFade) {
                    ar.fadeTo(L.fadeTime, af)
                } else {
                    ar.css("opacity", af)
                }
                aF = true
            }
            function aO() {
                aP();
                B.hide();
                if (L.bgFade) {
                    ar.fadeTo(L.fadeTime, 1)
                } else {
                    ar.css("opacity", 1)
                }
                aF = false;
                L.onRelease.call(g)
            }
            function av() {
                if (ax) {
                    aK(ae.getFixed());
                    P.show()
                }
            }
            function aJ() {
                ax = true;
                if (L.allowResize) {
                    aK(ae.getFixed());
                    P.show();
                    return true
                }
            }
            function aP() {
                ax = false;
                P.hide()
            }
            function aL(aU) {
                if (E === aU) {
                    aP()
                } else {
                    aJ()
                }
            }
            function aH() {
                aL(false);
                ay()
            }
            if (L.drawBorders) {
                aA = {top: aB("hline"), bottom: aB("hline bottom"), left: aB("vline"), right: aB("vline right")}
            }
            if (L.dragEdges) {
                aQ.t = aM("n");
                aQ.b = aM("s");
                aQ.r = aM("e");
                aQ.l = aM("w")
            }
            if (L.sideHandles) {
                aI(["n", "s", "e", "w"])
            }
            if (L.cornerHandles) {
                aI(["sw", "nw", "ne", "se"])
            }
            var az = aj().mousedown(c("move")).css({cursor: "move", position: "absolute", zIndex: 360});
            if (W.support) {
                az.bind("touchstart.jcrop", W.createDragger("move"))
            }
            l.append(az);
            aP();
            return{updateVisible: aR, update: aC, release: aO, refresh: ay, isAwake: function () {
                    return aF
                }, setCursor: function (aU) {
                    az.css("cursor", aU)
                }, enableHandles: aJ, enableOnly: function () {
                    ax = true
                }, showHandles: av, disableHandles: aP, animMode: aL, done: aH}
        }());
        var T = (function () {
            var aw = function () {
            }, ay = function () {
            }, ax = L.trackDocument;
            function aF() {
                b.css({zIndex: 450});
                if (ax) {
                    a(document).bind("mousemove", av).bind("mouseup", az)
                }
            }
            function aD() {
                b.css({zIndex: 290});
                if (ax) {
                    a(document).unbind("mousemove", av).unbind("mouseup", az)
                }
            }
            function av(aG) {
                aw(J(aG));
                return false
            }
            function az(aG) {
                aG.preventDefault();
                aG.stopPropagation();
                if (r) {
                    r = false;
                    ay(J(aG));
                    if (aa.isAwake()) {
                        L.onSelect.call(g, ac(ae.getFixed()))
                    }
                    aD();
                    aw = function () {
                    };
                    ay = function () {
                    }
                }
                return false
            }
            function aA(aH, aG) {
                r = true;
                aw = aH;
                ay = aG;
                aF();
                return false
            }
            function aE(aG) {
                aG.pageX = aG.originalEvent.changedTouches[0].pageX;
                aG.pageY = aG.originalEvent.changedTouches[0].pageY;
                return av(aG)
            }
            function aB(aG) {
                aG.pageX = aG.originalEvent.changedTouches[0].pageX;
                aG.pageY = aG.originalEvent.changedTouches[0].pageY;
                return az(aG)
            }
            function aC(aG) {
                b.css("cursor", aG)
            }
            if (W.support) {
                a(document).bind("touchmove", aE).bind("touchend", aB)
            }
            if (!ax) {
                b.mousemove(av).mouseup(az).mouseout(az)
            }
            ar.before(b);
            return{activateHandlers: aA, setCursor: aC}
        }());
        var aq = (function () {
            var ay = a('<input type="radio" />').css({position: "fixed", left: "-120px", width: "12px"}), aA = a("<div />").css({position: "absolute", overflow: "hidden"}).append(ay);
            function aw() {
                if (L.keySupport) {
                    ay.show();
                    ay.focus()
                }
            }
            function az(aB) {
                ay.hide()
            }
            function ax(aC, aB, aD) {
                if (L.allowMove) {
                    ae.moveOffset([aB, aD]);
                    aa.updateVisible()
                }
                aC.preventDefault();
                aC.stopPropagation()
            }
            function av(aC) {
                if (aC.ctrlKey) {
                    return true
                }
                ag = aC.shiftKey ? true : false;
                var aB = ag ? 10 : 1;
                switch (aC.keyCode) {
                    case 37:
                        ax(aC, -aB, 0);
                        break;
                    case 39:
                        ax(aC, aB, 0);
                        break;
                    case 38:
                        ax(aC, 0, -aB);
                        break;
                    case 40:
                        ax(aC, 0, aB);
                        break;
                    case 27:
                        aa.release();
                        break;
                    case 9:
                        return true
                }
                return false
            }
            if (L.keySupport) {
                ay.keydown(av).blur(az);
                if (ak || !L.fixedSupport) {
                    ay.css({position: "absolute", left: "-20px"});
                    aA.append(ay).insertBefore(ar)
                } else {
                    ay.insertBefore(ar)
                }
            }
            return{watchKeys: aw}
        }());
        function k(av) {
            ad.removeClass().addClass(F("holder")).addClass(av)
        }
        function t(aO, aC) {
            var aI = parseInt(aO[0], 10) / Q, ax = parseInt(aO[1], 10) / e, aH = parseInt(aO[2], 10) / Q, aw = parseInt(aO[3], 10) / e;
            if (E) {
                return
            }
            var aG = ae.flipCoords(aI, ax, aH, aw), aM = ae.getFixed(), aJ = [aM.x, aM.y, aM.x2, aM.y2], az = aJ, ay = L.animationDelay, aL = aG[0] - aJ[0], aB = aG[1] - aJ[1], aK = aG[2] - aJ[2], aA = aG[3] - aJ[3], aF = 0, aD = L.swingSpeed;
            x = az[0];
            y = az[1];
            aH = az[2];
            aw = az[3];
            aa.animMode(true);
            var av;
            function aE() {
                window.setTimeout(aN, ay)
            }
            var aN = (function () {
                return function () {
                    aF += (100 - aF) / aD;
                    az[0] = x + ((aF / 100) * aL);
                    az[1] = y + ((aF / 100) * aB);
                    az[2] = aH + ((aF / 100) * aK);
                    az[3] = aw + ((aF / 100) * aA);
                    if (aF >= 99.8) {
                        aF = 100
                    }
                    if (aF < 100) {
                        ao(az);
                        aE()
                    } else {
                        aa.done();
                        if (typeof (aC) === "function") {
                            aC.call(g)
                        }
                    }
                }
            }());
            aE()
        }
        function N(av) {
            ao([parseInt(av[0], 10) / Q, parseInt(av[1], 10) / e, parseInt(av[2], 10) / Q, parseInt(av[3], 10) / e])
        }
        function ao(av) {
            ae.setPressed([av[0], av[1]]);
            ae.setCurrent([av[2], av[3]]);
            aa.update()
        }
        function j() {
            return ac(ae.getFixed())
        }
        function am() {
            return ae.getFixed()
        }
        function v(av) {
            C(av);
            R()
        }
        function z() {
            L.disabled = true;
            aa.disableHandles();
            aa.setCursor("default");
            T.setCursor("default")
        }
        function Z() {
            L.disabled = false;
            R()
        }
        function n() {
            aa.done();
            T.activateHandlers(null, null)
        }
        function al() {
            ad.remove();
            ab.show();
            a(d).removeData("Jcrop")
        }
        function at(aw, ax) {
            aa.release();
            z();
            var av = new Image();
            av.onload = function () {
                var ay = av.width;
                var aA = av.height;
                var aB = L.boxWidth;
                var az = L.boxHeight;
                ar.width(ay).height(aA);
                ar.attr("src", aw);
                M.attr("src", aw);
                Y(ar, aB, az);
                U = ar.width();
                S = ar.height();
                M.width(U).height(S);
                b.width(U + (u * 2)).height(S + (u * 2));
                ad.width(U).height(S);
                Z();
                if (typeof (ax) === "function") {
                    ax.call(g)
                }
            };
            av.src = aw
        }
        function R(av) {
            if (L.allowResize) {
                if (av) {
                    aa.enableOnly()
                } else {
                    aa.enableHandles()
                }
            } else {
                aa.disableHandles()
            }
            T.setCursor(L.allowSelect ? "crosshair" : "default");
            aa.setCursor(L.allowMove ? "move" : "default");
            if (L.hasOwnProperty("setSelect")) {
                N(L.setSelect);
                aa.done();
                delete (L.setSelect)
            }
            if (L.hasOwnProperty("trueSize")) {
                Q = L.trueSize[0] / U;
                e = L.trueSize[1] / S
            }
            if (L.hasOwnProperty("bgColor")) {
                if (G() && L.fadeTime) {
                    ad.animate({backgroundColor: L.bgColor}, {queue: false, duration: L.fadeTime})
                } else {
                    ad.css("backgroundColor", L.bgColor)
                }
                delete (L.bgColor)
            }
            if (L.hasOwnProperty("bgOpacity")) {
                af = L.bgOpacity;
                if (aa.isAwake()) {
                    if (L.fadeTime) {
                        ar.fadeTo(L.fadeTime, af)
                    } else {
                        ad.css("opacity", L.opacity)
                    }
                }
                delete (L.bgOpacity)
            }
            A = L.maxSize[0] || 0;
            an = L.maxSize[1] || 0;
            p = L.minSize[0] || 0;
            X = L.minSize[1] || 0;
            if (L.hasOwnProperty("outerImage")) {
                ar.attr("src", L.outerImage);
                delete (L.outerImage)
            }
            aa.refresh()
        }
        if (W.support) {
            b.bind("touchstart", W.newSelection)
        }
        P.hide();
        R(true);
        var g = {setImage: at, animateTo: t, setSelect: N, setOptions: v, tellSelect: j, tellScaled: am, setClass: k, disable: z, enable: Z, cancel: n, release: aa.release, destroy: al, focus: aq.watchKeys, getBounds: function () {
                return[U * Q, S * e]
            }, getWidgetSize: function () {
                return[U, S]
            }, getScaleFactor: function () {
                return[Q, e]
            }, ui: {holder: ad, selection: B}};
        if (a.browser.msie) {
            ad.bind("selectstart", function () {
                return false
            })
        }
        ab.data("Jcrop", g);
        return g
    };
    a.fn.Jcrop = function (c, d) {
        function b(i) {
            var g = (typeof (c) === "object") ? c : {};
            var f = g.useImg || i.src;
            var e = new Image();
            e.onload = function () {
                function j() {
                    var l = a.Jcrop(i, g);
                    if (typeof (d) === "function") {
                        d.call(l)
                    }
                }
                function k() {
                    if (!e.width || !e.height) {
                        window.setTimeout(k, 50)
                    } else {
                        j()
                    }
                }
                window.setTimeout(k, 50)
            };
            e.src = f
        }
        this.each(function () {
            if (a(this).data("Jcrop")) {
                if (c === "api") {
                    return a(this).data("Jcrop")
                } else {
                    a(this).data("Jcrop").setOptions(c)
                }
            } else {
                b(this)
            }
        });
        return this
    };
    a.Jcrop.defaults = {allowSelect: true, allowMove: true, allowResize: true, trackDocument: true, baseClass: "jcrop", addClass: null, bgColor: "black", bgOpacity: 0.6, bgFade: false, borderOpacity: 0.4, handleOpacity: 0.5, handleSize: 9, handleOffset: 5, aspectRatio: 0, keySupport: true, cornerHandles: true, sideHandles: true, drawBorders: true, dragEdges: true, fixedSupport: true, touchSupport: null, boxWidth: 0, boxHeight: 0, boundary: 2, fadeTime: 400, animationDelay: 20, swingSpeed: 3, minSelect: [0, 0], maxSize: [0, 0], minSize: [0, 0], onChange: function () {
        }, onSelect: function () {
        }, onRelease: function () {
        }}
}(jQuery));
PrimeFaces.widget.ImageCropper = PrimeFaces.widget.DeferredWidget.extend({init: function (a) {
        this._super(a);
        this.image = $(PrimeFaces.escapeClientId(this.cfg.image));
        this.jqCoords = $(this.jqId + "_coords");
        var b = this;
        this.cfg.onSelect = function (d) {
            b.saveCoords(d)
        };
        this.cfg.onChange = function (d) {
            b.saveCoords(d)
        };
        this.renderDeferred()
    }, _render: function () {
        this.image.Jcrop(this.cfg)
    }, saveCoords: function (b) {
        var a = b.x + "_" + b.y + "_" + b.w + "_" + b.h;
        this.jqCoords.val(a)
    }});