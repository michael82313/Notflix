sub init()
    m.profileGrid = m.top.findNode("profileGrid")
    m.emailSubtitle = m.top.findNode("emailSubtitle")
    m.errorLabel = m.top.findNode("errorLabel")
    m.retryBorder = m.top.findNode("retryBorder")
    m.retryBg = m.top.findNode("retryBg")
    m.signOutBorder = m.top.findNode("signOutBorder")
    m.signOutBg = m.top.findNode("signOutBg")

    m.profilesData = []
    m.focusArea = "grid" ' "grid" | "retry" | "signOut"

    m.profileGrid.observeField("itemSelected", "onProfileSelected")
end sub

sub onAccountEmailChanged()
    if m.top.accountEmail <> ""
        m.emailSubtitle.text = "Signed in as " + m.top.accountEmail
    else
        m.emailSubtitle.text = ""
    end if
end sub

sub onErrorChanged()
    m.errorLabel.text = m.top.errorMessage
end sub

sub onProfilesChanged()
    profiles = m.top.profiles
    if profiles = invalid then profiles = []
    m.profilesData = profiles

    rootNode = CreateObject("roSGNode", "ContentNode")
    for each p in profiles
        node = rootNode.CreateChild("ContentNode")
        node.title = p.name
        node.AddFields({
            profileId: p.id
            avatarKey: p.avatarKey
            isKids: p.isKids
        })
    end for

    count = profiles.Count()
    if count > 0
        cols = count
        if cols > 5 then cols = 5
        m.profileGrid.numColumns = cols
        totalW = (cols * 232) + ((cols - 1) * 44)
        startX = Int((1920 - totalW) / 2)
        if startX < 90 then startX = 90
        m.profileGrid.translation = [startX, 380]
        m.profileGrid.content = rootNode
        m.focusArea = "grid"
    else
        m.profileGrid.content = rootNode
        m.focusArea = "retry"
    end if
    applyFocusState()
end sub

sub onFocusRequested()
    if m.profilesData.Count() > 0
        m.focusArea = "grid"
    else
        m.focusArea = "retry"
    end if
    applyFocusState()
end sub

sub applyFocusState()
    m.retryBorder.color = "0x2E2E3AFF"
    m.retryBg.color = "0x1A1A24FF"
    m.signOutBorder.color = "0x2E2E3AFF"
    m.signOutBg.color = "0x1A1A24FF"

    if m.focusArea = "grid" and m.profilesData.Count() > 0
        m.profileGrid.setFocus(true)
    else if m.focusArea = "retry"
        m.top.setFocus(true)
        m.retryBorder.color = "0xE50914FF"
        m.retryBg.color = "0x282838FF"
    else
        m.top.setFocus(true)
        m.signOutBorder.color = "0xE50914FF"
        m.signOutBg.color = "0x282838FF"
    end if
end sub

sub onProfileSelected()
    idx = m.profileGrid.itemSelected
    if idx >= 0 and idx < m.profilesData.Count()
        m.top.selectedProfile = m.profilesData[idx]
    end if
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false

    if m.focusArea = "grid"
        if key = "down"
            m.focusArea = "retry"
            applyFocusState()
            return true
        end if
    else
        if key = "up" and m.profilesData.Count() > 0
            m.focusArea = "grid"
            applyFocusState()
            return true
        else if key = "right" and m.focusArea = "retry"
            m.focusArea = "signOut"
            applyFocusState()
            return true
        else if key = "left" and m.focusArea = "signOut"
            m.focusArea = "retry"
            applyFocusState()
            return true
        else if key = "OK"
            if m.focusArea = "retry"
                m.top.retryRequested = true
            else if m.focusArea = "signOut"
                m.top.signOutRequested = true
            end if
            return true
        end if
    end if

    return false
end function
