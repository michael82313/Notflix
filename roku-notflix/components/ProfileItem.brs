sub init()
    m.focusRing = m.top.findNode("focusRing")
    m.avatarBg = m.top.findNode("avatarBg")
    m.initialLabel = m.top.findNode("initialLabel")
    m.kidsBadgeBg = m.top.findNode("kidsBadgeBg")
    m.kidsBadgeLabel = m.top.findNode("kidsBadgeLabel")
    m.nameLabel = m.top.findNode("nameLabel")
end sub

sub onItemContentChanged()
    c = m.top.itemContent
    if c = invalid then return

    nameStr = c.title
    if nameStr = invalid or nameStr = "" then nameStr = "Viewer"
    m.nameLabel.text = nameStr
    m.initialLabel.text = UCase(Left(nameStr.Trim(), 1))

    avatarKey = "crimson"
    if c.HasField("avatarKey") and c.avatarKey <> "" then avatarKey = LCase(c.avatarKey)

    if avatarKey = "sky" or avatarKey = "cyan" or avatarKey = "blue"
        m.avatarBg.color = "0x0284C7FF"
    else if avatarKey = "gold" or avatarKey = "amber"
        m.avatarBg.color = "0xD97706FF"
    else if avatarKey = "emerald" or avatarKey = "green"
        m.avatarBg.color = "0x15803DFF"
    else if avatarKey = "violet" or avatarKey = "purple"
        m.avatarBg.color = "0x7C3AEDFF"
    else
        m.avatarBg.color = "0xC8101CFF"
    end if

    isKids = false
    if c.HasField("isKids") then isKids = c.isKids
    m.kidsBadgeBg.visible = isKids
    m.kidsBadgeLabel.visible = isKids
end sub

sub onFocusPercentChanged()
    f = m.top.focusPercent
    m.focusRing.opacity = f
    if f > 0.5
        m.nameLabel.color = "0xF5F5F7FF"
    else
        m.nameLabel.color = "0xA0A3B1FF"
    end if
end sub
