param(
    [string]$ExePath,
    [string]$OutputPath
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

Add-Type -ReferencedAssemblies "$([System.Drawing.Bitmap].Assembly.Location)" @"
using System;
using System.Drawing;
using System.Runtime.InteropServices;

public class RemotePulseExeIcon
{
    [ComImport]
    [Guid("43826D1E-E718-42EE-BC55-A1E261C37BFE")]
    [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IShellItem
    {
    }

    [ComImport]
    [Guid("BCC18B79-BA16-442F-80C4-8A59C30C463B")]
    [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IShellItemImageFactory
    {
        int GetImage(
            SIZE size,
            SIIGBF flags,
            out IntPtr phbm
        );
    }

    [StructLayout(LayoutKind.Sequential)]
    struct SIZE
    {
        public int cx;
        public int cy;

        public SIZE(int x, int y)
        {
            cx = x;
            cy = y;
        }
    }

    [Flags]
    enum SIIGBF
    {
        SIIGBF_ICONONLY = 0x00000004,
        SIIGBF_BIGGERSIZEOK = 0x00000001
    }

    [DllImport(
        "shell32.dll",
        CharSet = CharSet.Unicode
    )]
    static extern int SHCreateItemFromParsingName(
        string pszPath,
        IntPtr pbc,
        ref Guid riid,
        out IShellItem ppv
    );

    [DllImport("gdi32.dll")]
    static extern bool DeleteObject(IntPtr hObject);

    public static void Save(
        string exePath,
        string outputPath
    )
    {
        Guid iid = typeof(IShellItemImageFactory).GUID;

        IShellItem item;

        int hr = SHCreateItemFromParsingName(
            exePath,
            IntPtr.Zero,
            ref iid,
            out item
        );

        if (hr != 0)
        {
            throw new Exception(
                "SHCreateItemFromParsingName failed: 0x"
                + hr.ToString("X8")
            );
        }

        IShellItemImageFactory factory =
            (IShellItemImageFactory)item;

        IntPtr hBitmap;

        hr = factory.GetImage(
            new SIZE(256, 256),
            SIIGBF.SIIGBF_ICONONLY |
            SIIGBF.SIIGBF_BIGGERSIZEOK,
            out hBitmap
        );

        if (hr != 0)
        {
            throw new Exception(
                "GetImage failed: 0x"
                + hr.ToString("X8")
            );
        }

        try
        {
            using (Bitmap bitmap = Bitmap.FromHbitmap(hBitmap))
            {
                bitmap.Save(outputPath);
            }
        }
        finally
        {
            DeleteObject(hBitmap);
        }
    }
}
"@

[RemotePulseExeIcon]::Save(
    $ExePath,
    $OutputPath
)

Write-Host "EXE icon created:"
Write-Host $OutputPath