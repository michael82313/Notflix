' =============================================================================
' Notflix for Roku TV — Main BrightScript Entrypoint
' =============================================================================

sub Main(args as dynamic)
    screen = CreateObject("roSGScreen")
    m.port = CreateObject("roMessagePort")
    screen.SetMessagePort(m.port)

    scene = screen.CreateScene("MainScene")
    scene.observeField("exitChannel", m.port)
    screen.Show()

    ' Pass launch arguments (e.g., deep links) if provided
    if args <> invalid and Type(args) = "roAssociativeArray"
        if args.contentId <> invalid and args.contentId <> ""
            scene.launchArgs = args
        end if
    end if

    while true
        msg = wait(0, m.port)
        msgType = Type(msg)
        if msgType = "roSGScreenEvent"
            if msg.IsScreenClosed()
                return
            end if
        else if msgType = "roSGNodeEvent"
            if msg.GetField() = "exitChannel" and msg.GetData() = true
                screen.Close()
                return
            end if
        end if
    end while
end sub
