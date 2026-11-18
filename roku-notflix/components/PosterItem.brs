sub init()
    m.focusFrame = m.top.findNode("focusFrame")
    m.posterImg = m.top.findNode("posterImg")
    m.titleLabel = m.top.findNode("titleLabel")
    m.typeBadgeBg = m.top.findNode("typeBadgeBg")
    m.typeBadgeLabel = m.top.findNode("typeBadgeLabel")
    m.progressTrack = m.top.findNode("progressTrack")
    m.progressFill = m.top.findNode("progressFill")
end sub

sub onItemContentChanged()
    content = m.top.itemContent
    if content = invalid then return

    uri = content.HDPosterUrl
    if uri = invalid or uri = "" then uri = "pkg:/images/poster_fallback.jpg"
    m.posterImg.uri = uri

    m.titleLabel.text = content.title

    mediaType = ""
    if content.HasField("mediaType") then mediaType = content.mediaType
    if mediaType = "series"
        m.typeBadgeBg.visible = true
        m.typeBadgeLabel.visible = true
        m.typeBadgeLabel.text = "SERIES"
    else
        m.typeBadgeBg.visible = false
        m.typeBadgeLabel.visible = false
    end if

    progFrac = 0.0
    if content.HasField("progressFraction") then progFrac = content.progressFraction
    if progFrac > 0.02 and progFrac < 0.98
        m.progressTrack.visible = true
        m.progressFill.visible = true
        barW = Int(216.0 * progFrac)
        if barW < 10 then barW = 10
        m.progressFill.width = barW
    else
        m.progressTrack.visible = false
        m.progressFill.visible = false
    end if
end sub

sub onFocusPercentChanged()
    f = m.top.focusPercent
    rf = m.top.rowFocusPercent
    ' When used inside MarkupGrid, rowFocusPercent stays 0 — handle both RowList and MarkupGrid cleanly
    effective = f
    if rf > 0.0
        effective = f * rf
    end if
    m.focusFrame.opacity = effective
    scaleAmt = 1.0 + (0.04 * effective)
    m.top.scale = [scaleAmt, scaleAmt]
end sub
