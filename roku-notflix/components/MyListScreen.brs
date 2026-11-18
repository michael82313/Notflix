sub init()
    m.headerLabel = m.top.findNode("headerLabel")
    m.emptyLabel = m.top.findNode("emptyLabel")
    m.listGrid = m.top.findNode("listGrid")

    m.savedItems = []
    m.lastFocusedIdx = 0

    m.listGrid.observeField("itemFocused", "onItemFocused")
    m.listGrid.observeField("itemSelected", "onItemSelected")
end sub

sub onProfileNameChanged()
    pName = SafeStr(m.top.profileName, "")
    if pName <> ""
        m.headerLabel.text = pName + "'s My List"
    else
        m.headerLabel.text = "My List"
    end if
end sub

sub onItemsChanged()
    arr = m.top.items
    if arr = invalid then arr = []
    m.savedItems = arr

    if arr.Count() = 0
        m.listGrid.content = invalid
        m.emptyLabel.visible = true
        return
    end if

    m.emptyLabel.visible = false
    rootNode = CreateObject("roSGNode", "ContentNode")
    for each item in arr
        node = rootNode.CreateChild("ContentNode")
        node.title = item.title
        node.HDPosterUrl = item.resolvedPosterUri
        node.SDPosterUrl = item.resolvedPosterUri
        progFrac = 0.0
        if item.progressFraction <> invalid then progFrac = item.progressFraction
        node.AddFields({
            contentId: item.id
            mediaType: item.mediaType
            progressFraction: progFrac
        })
    end for
    m.listGrid.content = rootNode

    if m.lastFocusedIdx >= arr.Count()
        m.lastFocusedIdx = 0
    end if
    m.listGrid.jumpToItem = m.lastFocusedIdx
end sub

sub onFocusRequested()
    if m.savedItems.Count() > 0
        m.listGrid.setFocus(true)
        if m.lastFocusedIdx < m.savedItems.Count()
            m.listGrid.jumpToItem = m.lastFocusedIdx
        end if
    else
        m.top.setFocus(true)
    end if
end sub

sub onItemFocused()
    idx = m.listGrid.itemFocused
    if idx >= 0 then m.lastFocusedIdx = idx
end sub

sub onItemSelected()
    idx = m.listGrid.itemSelected
    if idx >= 0 and idx < m.savedItems.Count()
        m.lastFocusedIdx = idx
        m.top.selectedItem = m.savedItems[idx]
    end if
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false

    if key = "left"
        if m.savedItems.Count() = 0 or (m.listGrid.itemFocused mod 6) = 0
            m.top.requestNavFocus = true
            return true
        end if
    end if

    return false
end function
