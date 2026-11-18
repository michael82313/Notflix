sub init()
    m.heroBackdrop = m.top.findNode("heroBackdrop")
    m.heroBadge = m.top.findNode("heroBadge")
    m.heroTitle = m.top.findNode("heroTitle")
    m.heroMeta = m.top.findNode("heroMeta")
    m.heroSynopsis = m.top.findNode("heroSynopsis")
    m.profilePillLabel = m.top.findNode("profilePillLabel")
    m.profilePillBg = m.top.findNode("profilePillBg")
    m.emptyLabel = m.top.findNode("emptyLabel")
    m.homeRowList = m.top.findNode("homeRowList")

    m.rowsData = []
    m.lastFocusedRow = 0
    m.lastFocusedCol = 0

    m.homeRowList.observeField("rowItemFocused", "onRowItemFocused")
    m.homeRowList.observeField("rowItemSelected", "onRowItemSelected")
end sub

sub onProfileInfoChanged()
    pName = m.top.profileName
    if pName = "" then pName = "Profile"
    if m.top.isKidsProfile
        m.profilePillLabel.text = pName + "  •  KIDS"
        m.profilePillLabel.color = "0x38BDF8FF"
    else
        m.profilePillLabel.text = "Watching: " + pName
        m.profilePillLabel.color = "0xF5F5F7FF"
    end if
end sub

sub onFeedDataChanged()
    feed = m.top.feedData
    if feed = invalid or feed.rows = invalid or feed.rows.Count() = 0
        m.rowsData = []
        m.homeRowList.content = invalid
        m.emptyLabel.visible = true
        m.emptyLabel.text = "No playable titles found in Notflix catalog for this profile yet."
        if feed <> invalid and feed.heroItem <> invalid
            updateHero(feed.heroItem, "FEATURED ON NOTFLIX")
        end if
        return
    end if

    m.emptyLabel.visible = false
    m.rowsData = feed.rows

    rootNode = CreateObject("roSGNode", "ContentNode")
    for each rowObj in m.rowsData
        rowNode = rootNode.CreateChild("ContentNode")
        rowNode.title = rowObj.title
        for each item in rowObj.items
            itemNode = rowNode.CreateChild("ContentNode")
            itemNode.title = item.title
            itemNode.HDPosterUrl = item.resolvedPosterUri
            itemNode.SDPosterUrl = item.resolvedPosterUri
            itemNode.AddFields({
                contentId: item.id
                mediaType: item.mediaType
                progressFraction: item.progressFraction
            })
        end for
    end for

    m.homeRowList.content = rootNode

    ' Restore previous focus index safely if still in range
    if m.lastFocusedRow >= m.rowsData.Count()
        m.lastFocusedRow = 0
        m.lastFocusedCol = 0
    else if m.lastFocusedCol >= m.rowsData[m.lastFocusedRow].items.Count()
        m.lastFocusedCol = 0
    end if

    m.homeRowList.jumpToRowItem = [m.lastFocusedRow, m.lastFocusedCol]

    if feed.heroItem <> invalid
        updateHero(feed.heroItem, "FEATURED ON NOTFLIX")
    else
        updateHero(m.rowsData[0].items[0], UCase(m.rowsData[0].title))
    end if
end sub

sub onFocusRequested()
    if m.rowsData.Count() > 0
        m.homeRowList.setFocus(true)
        m.homeRowList.jumpToRowItem = [m.lastFocusedRow, m.lastFocusedCol]
    else
        m.top.setFocus(true)
    end if
end sub

sub onRowItemFocused()
    pair = m.homeRowList.rowItemFocused
    if pair = invalid or pair.Count() < 2 then return
    rIdx = pair[0]
    cIdx = pair[1]
    if rIdx >= 0 and rIdx < m.rowsData.Count()
        rowObj = m.rowsData[rIdx]
        if cIdx >= 0 and cIdx < rowObj.items.Count()
            m.lastFocusedRow = rIdx
            m.lastFocusedCol = cIdx
            updateHero(rowObj.items[cIdx], UCase(rowObj.title))
        end if
    end if
end sub

sub onRowItemSelected()
    pair = m.homeRowList.rowItemSelected
    if pair = invalid or pair.Count() < 2 then return
    rIdx = pair[0]
    cIdx = pair[1]
    if rIdx >= 0 and rIdx < m.rowsData.Count()
        rowObj = m.rowsData[rIdx]
        if cIdx >= 0 and cIdx < rowObj.items.Count()
            m.lastFocusedRow = rIdx
            m.lastFocusedCol = cIdx
            m.top.selectedItem = rowObj.items[cIdx]
        end if
    end if
end sub

sub updateHero(item as object, badgeText as string)
    if item = invalid then return

    m.heroBackdrop.uri = item.resolvedBackdropUri
    m.heroBadge.text = badgeText
    m.heroTitle.text = item.title

    metaParts = []
    if item.releaseYear > 0 then metaParts.Push(item.releaseYear.ToStr())
    if item.ageRating <> "" then metaParts.Push(item.ageRating)
    if item.mediaType = "series"
        metaParts.Push("TV Series")
    else if item.runtimeMinutes > 0
        metaParts.Push(item.runtimeMinutes.ToStr() + " min")
    end if
    if item.genres <> invalid and item.genres.Count() > 0
        gLine = ""
        limitG = item.genres.Count()
        if limitG > 3 then limitG = 3
        for i = 0 to limitG - 1
            if i > 0 then gLine = gLine + ", "
            gLine = gLine + item.genres[i]
        end for
        metaParts.Push(gLine)
    end if

    metaOut = ""
    for i = 0 to metaParts.Count() - 1
        if i > 0 then metaOut = metaOut + "   •   "
        metaOut = metaOut + metaParts[i]
    end for
    m.heroMeta.text = metaOut
    m.heroSynopsis.text = item.synopsis
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false

    ' Pressing Left on the first column of a row smoothly opens the Left TV Navigation Rail
    if key = "left"
        if m.rowsData.Count() = 0 or m.lastFocusedCol = 0
            m.top.requestNavFocus = true
            return true
        end if
    end if

    return false
end function
