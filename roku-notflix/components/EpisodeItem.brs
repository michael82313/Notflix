sub init()
    m.focusBorder = m.top.findNode("focusBorder")
    m.rowBg = m.top.findNode("rowBg")
    m.thumbImg = m.top.findNode("thumbImg")
    m.epProgTrack = m.top.findNode("epProgTrack")
    m.epProgFill = m.top.findNode("epProgFill")
    m.epTitle = m.top.findNode("epTitle")
    m.epMeta = m.top.findNode("epMeta")
    m.epSynopsis = m.top.findNode("epSynopsis")
    m.epStatus = m.top.findNode("epStatus")
end sub

sub onItemContentChanged()
    c = m.top.itemContent
    if c = invalid then return

    uri = c.HDPosterUrl
    if uri = invalid or uri = "" then uri = "pkg:/images/episode_fallback.jpg"
    m.thumbImg.uri = uri

    m.epTitle.text = c.title
    m.epSynopsis.text = c.description

    metaStr = ""
    if c.HasField("runtimeLabel") then metaStr = c.runtimeLabel
    m.epMeta.text = metaStr

    statusText = ""
    statusColor = "0x22C55EFF"
    if c.HasField("statusLabel") then statusText = c.statusLabel
    if c.HasField("statusColor") and c.statusColor <> "" then statusColor = c.statusColor
    m.epStatus.text = statusText
    m.epStatus.color = statusColor

    progFrac = 0.0
    if c.HasField("progressFraction") then progFrac = c.progressFraction
    if progFrac > 0.02 and progFrac < 0.99
        m.epProgTrack.visible = true
        m.epProgFill.visible = true
        m.epProgFill.width = Int(256.0 * progFrac)
    else
        m.epProgTrack.visible = false
        m.epProgFill.visible = false
    end if
end sub

sub onFocusPercentChanged()
    f = m.top.focusPercent
    m.focusBorder.opacity = f
    if f > 0.5
        m.rowBg.color = "0x232330FF"
    else
        m.rowBg.color = "0x15151DFF"
    end if
end sub
