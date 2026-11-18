sub init()
    m.emailText = ""
    m.passwordText = ""
    m.focusIdx = 0 ' 0=Email, 1=Password, 2=SignIn, 3=ResetPassword
    m.activeDialogField = ""

    m.emailBorder = m.top.findNode("emailBorder")
    m.emailBg = m.top.findNode("emailBg")
    m.emailValue = m.top.findNode("emailValue")

    m.passBorder = m.top.findNode("passBorder")
    m.passBg = m.top.findNode("passBg")
    m.passValue = m.top.findNode("passValue")

    m.signInBorder = m.top.findNode("signInBorder")
    m.signInBg = m.top.findNode("signInBg")
    m.signInLabel = m.top.findNode("signInLabel")

    m.resetBorder = m.top.findNode("resetBorder")
    m.resetBg = m.top.findNode("resetBg")
    m.resetLabel = m.top.findNode("resetLabel")

    m.statusLabel = m.top.findNode("statusLabel")

    updateFocusVisuals()
end sub

sub onFocusRequested()
    m.top.setFocus(true)
    updateFocusVisuals()
end sub

sub onBusyChanged()
    if m.top.isBusy
        m.signInLabel.text = "SIGNING IN..."
    else
        m.signInLabel.text = "SIGN IN"
    end if
end sub

sub onStatusChanged()
    if m.top.errorMessage <> ""
        m.statusLabel.color = "0xFF5370FF"
        m.statusLabel.text = m.top.errorMessage
    else if m.top.infoMessage <> ""
        m.statusLabel.color = "0x22C55EFF"
        m.statusLabel.text = m.top.infoMessage
    else
        m.statusLabel.text = ""
    end if
end sub

sub updateFocusVisuals()
    inactiveBorder = "0x2E2E3AFF"
    activeBorder = "0xF5F5F7FF"
    crimsonBorder = "0xE50914FF"

    m.emailBorder.color = inactiveBorder
    m.passBorder.color = inactiveBorder
    m.signInBorder.color = inactiveBorder
    m.resetBorder.color = inactiveBorder

    m.emailBg.color = "0x1A1A24FF"
    m.passBg.color = "0x1A1A24FF"
    m.signInBg.color = "0xB80710FF"
    m.resetBg.color = "0x1A1A24FF"

    if m.focusIdx = 0
        m.emailBorder.color = crimsonBorder
        m.emailBg.color = "0x252534FF"
    else if m.focusIdx = 1
        m.passBorder.color = crimsonBorder
        m.passBg.color = "0x252534FF"
    else if m.focusIdx = 2
        m.signInBorder.color = activeBorder
        m.signInBg.color = "0xE50914FF"
    else if m.focusIdx = 3
        m.resetBorder.color = activeBorder
        m.resetBg.color = "0x262638FF"
    end if

    if m.emailText <> ""
        m.emailValue.text = m.emailText
        m.emailValue.color = "0xF5F5F7FF"
    else
        m.emailValue.text = "Select to enter email..."
        m.emailValue.color = "0x7E8294FF"
    end if

    if m.passwordText <> ""
        masked = ""
        for i = 1 to Len(m.passwordText)
            masked = masked + "•"
        end for
        m.passValue.text = masked
        m.passValue.color = "0xF5F5F7FF"
    else
        m.passValue.text = "Select to enter password..."
        m.passValue.color = "0x7E8294FF"
    end if
end sub

sub openInputDialog(fieldType as string)
    scene = m.top.getScene()
    if scene = invalid then return

    m.activeDialogField = fieldType
    dlg = CreateObject("roSGNode", "KeyboardDialog")
    if fieldType = "email"
        dlg.title = "Enter Notflix Email"
        dlg.text = m.emailText
        dlg.keyboardDomain = "email"
    else
        dlg.title = "Enter Password"
        dlg.text = m.passwordText
        if dlg.keyboard <> invalid and dlg.keyboard.textEditBox <> invalid
            dlg.keyboard.textEditBox.secureMode = true
        end if
    end if
    dlg.buttons = ["Done", "Cancel"]
    dlg.observeField("buttonSelected", "onKeyboardButtonSelected")
    dlg.observeField("wasClosed", "onKeyboardClosed")

    m.inputDialog = dlg
    scene.dialog = dlg
end sub

sub onKeyboardButtonSelected()
    if m.inputDialog = invalid then return
    btnIdx = m.inputDialog.buttonSelected
    if btnIdx = 0
        entered = m.inputDialog.text
        if m.activeDialogField = "email"
            m.emailText = entered.Trim()
            m.focusIdx = 1
        else if m.activeDialogField = "password"
            m.passwordText = entered
            m.focusIdx = 2
        end if
        updateFocusVisuals()
    end if
    closeInputDialog()
end sub

sub onKeyboardClosed()
    m.inputDialog = invalid
    m.top.setFocus(true)
    updateFocusVisuals()
end sub

sub closeInputDialog()
    scene = m.top.getScene()
    if scene <> invalid
        scene.dialog = invalid
    end if
    m.inputDialog = invalid
    m.top.setFocus(true)
    updateFocusVisuals()
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if m.top.isBusy then return true

    if key = "up"
        if m.focusIdx > 0
            m.focusIdx = m.focusIdx - 1
            updateFocusVisuals()
        end if
        return true
    else if key = "down"
        if m.focusIdx < 3
            m.focusIdx = m.focusIdx + 1
            updateFocusVisuals()
        end if
        return true
    else if key = "OK"
        if m.focusIdx = 0
            openInputDialog("email")
        else if m.focusIdx = 1
            openInputDialog("password")
        else if m.focusIdx = 2
            m.top.errorMessage = ""
            m.top.infoMessage = ""
            m.top.signInRequest = { email: m.emailText, password: m.passwordText }
        else if m.focusIdx = 3
            m.top.errorMessage = ""
            m.top.infoMessage = ""
            m.top.resetPasswordRequest = m.emailText
        end if
        return true
    end if

    return false
end function
