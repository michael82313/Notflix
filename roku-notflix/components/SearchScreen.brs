sub init()
    m.searchKeyboard = m.top.findNode("searchKeyboard")
    m.clearBorder = m.top.findNode("clearBorder")
    m.clearBg = m.top.findNode("clearBg")
    m.resultsHeader = m.top.findNode("resultsHeader")
    m.emptyResultsLabel = m.top.findNode("emptyResultsLabel")
    m.resultsGrid = m.top.findNode("resultsGrid")

    m.filteredItems = []
    ' focusZone: "keyboard", "clear", "grid"
    m.focusZone = "keyboard"
    m.lastGridIndex = 0

    m.searchKeyboard.observeField("text", "onSearchTextChanged")
    m.resultsGrid.observeField("itemSelected", "onResultSelected")
    m.resultsGrid.observeField("itemFocused", "onResultFocused")
end sub

sub onCatalogOrProfileChanged()
    runFilter()
end sub

sub onSearchTextChanged()
    runFilter()
end sub

sub onFocusRequested()
    if m.focusZone = "grid" and m.filteredItems.Count() > 0
        m.resultsGrid.setFocus(true)
        if m.lastGridIndex < m.filteredItems.Count()
            m.resultsGrid.jumpToItem = m.lastGridIndex
        end if
    else if m.focusZone = "clear"
        m.top.setFocus(true)
    else
        m.focusZone = "keyboard"
        m.searchKeyboard.setFocus(true)
    end if
    updateClearVisual()
end sub

sub runFilter()
    cat = m.top.catalog
    if cat = invalid then cat = []
    isKids = m.top.isKidsProfile
    q = LCase(SafeStr(m.searchKeyboard.text, "").Trim())

    matches = []
    for each item in cat
        if SafeBool(item.isVisible, true) and not SafeBool(item.isComingSoon, false)
            if (not isKids) or SafeBool(item.isKidsFriendly, false)
                if q = "" or ItemMatchesQuery(item, q)
                    matches.Push(item)
                end if
            end if
        end if
    end for

    m.filteredItems = matches

    if q = ""
        m.resultsHeader.text = "Browse All Titles (" + matches.Count().ToStr() + ")"
    else
        m.resultsHeader.text = "Results for " + Chr(34) + m.searchKeyboard.text.Trim() + Chr(34) + " (" + matches.Count().ToStr() + ")"
    end if

    if matches.Count() = 0
        m.resultsGrid.content = invalid
        m.emptyResultsLabel.visible = true
        if q = ""
            m.emptyResultsLabel.text = "No playable titles are available in Notflix for this profile yet."
        else
            m.emptyResultsLabel.text = "No movies or TV shows matched " + Chr(34) + m.searchKeyboard.text.Trim() + Chr(34) + ". Try another title or genre."
        end if
        return
    end if

    m.emptyResultsLabel.visible = false
    rootNode = CreateObject("roSGNode", "ContentNode")
    for each item in matches
        node = rootNode.CreateChild("ContentNode")
        node.title = item.title
        node.HDPosterUrl = item.resolvedPosterUri
        node.SDPosterUrl = item.resolvedPosterUri
        node.AddFields({
            contentId: item.id
            mediaType: item.mediaType
            progressFraction: 0.0
        })
    end for
    m.resultsGrid.content = rootNode
end sub

function ItemMatchesQuery(item as object, lowerQuery as string) as boolean
    if Instr(1, LCase(SafeStr(item.title)), lowerQuery) > 0 then return true
    if Instr(1, LCase(SafeStr(item.synopsis)), lowerQuery) > 0 then return true
    if Instr(1, LCase(SafeStr(item.mediaType)), lowerQuery) > 0 then return true
    if item.genres <> invalid
        for each g in item.genres
            if Instr(1, LCase(g), lowerQuery) > 0 then return true
        end for
    end if
    if item.tags <> invalid
        for each t in item.tags
            if Instr(1, LCase(t), lowerQuery) > 0 then return true
        end for
    end if
    return false
end function

sub onResultFocused()
    idx = m.resultsGrid.itemFocused
    if idx >= 0 then m.lastGridIndex = idx
end sub

sub onResultSelected()
    idx = m.resultsGrid.itemSelected
    if idx >= 0 and idx < m.filteredItems.Count()
        m.lastGridIndex = idx
        m.top.selectedItem = m.filteredItems[idx]
    end if
end sub

sub updateClearVisual()
    if m.focusZone = "clear"
        m.clearBorder.color = "0xE50914FF"
        m.clearBg.color = "0x272738FF"
    else
        m.clearBorder.color = "0x2E2E3AFF"
        m.clearBg.color = "0x1A1A24FF"
    end if
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false

    if m.focusZone = "keyboard"
        if key = "right"
            if m.filteredItems.Count() > 0
                m.focusZone = "grid"
                m.resultsGrid.setFocus(true)
                updateClearVisual()
                return true
            end if
        else if key = "down"
            m.focusZone = "clear"
            m.top.setFocus(true)
            updateClearVisual()
            return true
        else if key = "left"
            m.top.requestNavFocus = true
            return true
        end if
    else if m.focusZone = "clear"
        if key = "up"
            m.focusZone = "keyboard"
            m.searchKeyboard.setFocus(true)
            updateClearVisual()
            return true
        else if key = "right"
            if m.filteredItems.Count() > 0
                m.focusZone = "grid"
                m.resultsGrid.setFocus(true)
                updateClearVisual()
                return true
            end if
        else if key = "left"
            m.top.requestNavFocus = true
            return true
        else if key = "OK"
            m.searchKeyboard.text = ""
            m.focusZone = "keyboard"
            m.searchKeyboard.setFocus(true)
            updateClearVisual()
            return true
        end if
    else if m.focusZone = "grid"
        if key = "left"
            ' Column 0 in a 4-column grid moves back to the MiniKeyboard
            if (m.resultsGrid.itemFocused mod 4) = 0
                m.focusZone = "keyboard"
                m.searchKeyboard.setFocus(true)
                updateClearVisual()
                return true
            end if
        else if key = "back"
            m.focusZone = "keyboard"
            m.searchKeyboard.setFocus(true)
            updateClearVisual()
            return true
        end if
    end if

    return false
end function
