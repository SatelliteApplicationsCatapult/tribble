package org.catapult.sa.tribble.mutators

import org.catapult.sa.tribble.CommonBytes._

import scala.util.Random

/**
  * Mutator that adds known file type prefixes on to the 
  */
class Prefix extends Mutator {
  override def mutate(in: Array[Byte], rand: Random): Array[Byte] = {
    val newPrefix = Prefix.byteMarkers(rand.nextInt(Prefix.byteMarkers.length))
    Array.concat(newPrefix, in)
  }
}

object Prefix {
  val byteMarkers : List[Array[Byte]] = List[Array[Byte]] (
    a(0xed, 0xab, 0xee, 0xdb), //RedHat Package Manager (RPM), package 
    a(0x53, 0x50, 0x30, 0x31), //Amazon Kindle Update Package 
    a(0x00), //IBM Storyboard bitmap file
    a(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00), //PalmPilot Database/Document File
    a(0xBE, 0xBA, 0xFE, 0xCA), //Palm Desktop Calendar Archive
    a(0x00, 0x01, 0x42, 0x44), //Palm Desktop To Do Archive
    a(0x00, 0x01, 0x44, 0x54), //Palm Desktop Calendar Archive
    a(0x00, 0x01, 0x00, 0x00), //Palm Desktop Data File (Access format),
    a(0x00, 0x00, 0x01, 0x00), //Computer icon encod0xed in ICO file format
    a(0x66, 0x74, 0x79, 0x70, 0x33, 0x67), //3rd Generation Partnership Project 3GPP and 3GPP2 multimedia files
    a(0x1F, 0x9D), //compress0xed file (often tar zip),
    a(0x1F, 0xA0), //Compress0xed file (often tar zip),
    a(0x42, 0x41, 0x43, 0x4B, 0x4D, 0x49, 0x4B, 0x45, 0x44, 0x49, 0x53, 0x4B), //File or tape containing a backup done with AmiBack on an Amiga.
    a(0x42, 0x5A, 0x68), //Compress0xed file using Bzip2 algorithm
    a(0x47, 0x49, 0x46, 0x38, 0x37, 0x61), //Image file encod0xed in the Graphics Interchange Format (GIF),
    a(0x47, 0x49, 0x46, 0x38, 0x39, 0x61), //Image file encod0xed in the Graphics Interchange Format (GIF),
    a(0x49, 0x49, 0x2A, 0x00), //Tagg0xed Image File Format(little endian format),
    a(0x4D, 0x4D, 0x00, 0x2A), //Tagg0xed Image File Format(big endian format),
    a(0x49, 0x49, 0x2A, 0x00, 0x10, 0x00, 0x00, 0x00), //Canon RAW Format Version 2
    a(0x43, 0x52), //Canon's RAW format is bas0xed on the TI0xFF file format
    a(0x80, 0x2A, 0x5F, 0xD7), //Kodak Cineon image
    a(0x52, 0x4E, 0x43, 0x01, 0x52, 0x4E, 0x43, 0x02), //Compress0xed file using Rob Northen Compression (version 1 and 2), algorithm
    a(0x53, 0x44, 0x50, 0x58), //SMPTE DPX image(big endian format),
    a(0x58, 0x50, 0x44, 0x53), //SMPTE DPX image(little endian format),
    a(0x76, 0x2F, 0x31, 0x01), //OpenEXR image
    a(0x42, 0x50, 0x47, 0xFB), //Better Portable Graphics format
    a(0xFF, 0xD8, 0xFF, 0xDB ), //JPEG raw or in the JFIF or Exif file format
    a(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x00, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01 ), //JPEG raw or in the JFIF or Exif file format
    a(0xFF, 0xD8, 0xFF, 0xE1, 0x00, 0x00, 0x45, 0x78, 0x69, 0x66, 0x00, 0x00), //JPEG raw or in the JFIF or Exif file format
    a(0x46, 0x4F, 0x52, 0x4D, 0x00, 0x00, 0x00, 0x00, 0x49, 0x4C, 0x42, 0x4D), //I0xFF Interleav0xed Bitmap Image
    a(0x46, 0x4F, 0x52, 0x4D, 0x00, 0x00, 0x00, 0x00, 0x38, 0x53, 0x56, 0x58), //I0xFF 8-Bit Sampl0xed Voice
    a(0x46, 0x4F, 0x52, 0x4D, 0x00, 0x00, 0x00, 0x00, 0x41, 0x43, 0x42, 0x4D), //Amiga Contiguous Bitmap
    a(0x46, 0x4F, 0x52, 0x4D, 0x00, 0x00, 0x00, 0x00, 0x41, 0x4E, 0x42, 0x4D), //I0xFF Animat0xed Bitmap
    a(0x46, 0x4F, 0x52, 0x4D, 0x00, 0x00, 0x00, 0x00, 0x41, 0x4E, 0x49, 0x4D), //I0xFF CEL Animation
    a(0x46, 0x4F, 0x52, 0x4D, 0x00, 0x00, 0x00, 0x00, 0x46, 0x41, 0x58, 0x58), //I0xFF Facsimile Image
    a(0x46, 0x4F, 0x52, 0x4D, 0x00, 0x00, 0x00, 0x00, 0x46, 0x54, 0x58, 0x54), //I0xFF Formatt0xed Text
    a(0x46, 0x4F, 0x52, 0x4D, 0x00, 0x00, 0x00, 0x00, 0x53, 0x4D, 0x55, 0x53), //I0xFF Simple Musical Score
    a(0x46, 0x4F, 0x52, 0x4D, 0x00, 0x00, 0x00, 0x00, 0x43, 0x4D, 0x55, 0x53), //I0xFF Musical Score
    a(0x46, 0x4F, 0x52, 0x4D, 0x00, 0x00, 0x00, 0x00, 0x59, 0x55, 0x56, 0x4E), //I0xFF YUV Image
    a(0x46, 0x4F, 0x52, 0x4D, 0x00, 0x00, 0x00, 0x00, 0x46, 0x41, 0x4E, 0x54), //Amiga Fantavision Movie
    a(0x46, 0x4F, 0x52, 0x4D, 0x00, 0x00, 0x00, 0x00, 0x41, 0x49, 0x46, 0x46), //Audio Interchange File Format
    a(0x49, 0x4E, 0x44, 0x58), //Index file to a file or tape containing a backup done with AmiBack on an Amiga.
    a(0x4C, 0x5A, 0x49, 0x50), //lzip compress0xed file
    a(0x4D, 0x5A), //DOS MZ executable file format and its descendants (including NE and PE),
    a(0x50, 0x4B, 0x03, 0x04, 0x50, 0x4B, 0x05, 0x06), //zip file format and formats bas0xed on it, such as JAR, ODF, OOXML (empty archive),
    a(0x50, 0x4B, 0x03, 0x04, 0x50, 0x4B, 0x07, 0x08), //zip file format and formats bas0xed on it, such as JAR, ODF, OOXML (spann0xed archive),
    a(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00), //RAR archive version 1.0x50 onwards
    a(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0x00), //RAR archive version 5.0 onwards
    a(0x7F, 0x45, 0x4C, 0x46), //Executable and Linkable Format
    a(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A), //Image encod0xed in the Portable Network Graphics format
    a(0xCA, 0xFE, 0xBA, 0xBE), //Java class file, Mach-O Fat Binary
    a(0xEF, 0xBB, 0xBF), //UTF-8 encod0xed Unico0xde byte order mark, commonly seen in text files.
    a(0xFE, 0xED, 0xFA, 0xCE), //Mach-O binary (32-bit),
    a(0xFE, 0xED, 0xFA, 0xCF), //Mach-O binary (64-bit),
    a(0xCE, 0xFA, 0xED, 0xFE), //Mach-O binary (reverse byte ordering scheme, 32-bit),
    a(0xCF, 0xFA, 0xED, 0xFE), //Mach-O binary (reverse byte ordering scheme, 64-bit),
    a(0xFF, 0xFE), //Byte-order mark for text file encod0xed in little-endian 16-bit Unico0xde Transfer Format
    a(0xFF, 0xFE, 0x00, 0x00), //Byte-order mark for text file encod0xed in little-endian 32-bit Unico0xde Transfer Format
    a(0x25, 0x21, 0x50, 0x53), //PostScript document
    a(0x25, 0x50, 0x44, 0x46), //P0xDF document
    a(0x30, 0x26, 0xB2, 0x75, 0x8E, 0x66, 0xCF, 0x11, 0xA6, 0xD9, 0x00, 0xAA, 0x00, 0x62, 0xCE, 0x6C), //Advanc0xed Systems Format
    a(0x24, 0x53, 0x44, 0x49, 0x30, 0x30, 0x30, 0x31), //System Deployment Image, a disk image format us0xed by Microsoft
    a(0x4F, 0x67, 0x67, 0x53), //Ogg, an open sour0xce media container format
    a(0x38, 0x42, 0x50, 0x53), //Photoshop Document file, Ado0xbe Photoshop's native file format
    a(0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x41, 0x56, 0x45), //Waveform Audio File Format
    a(0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x41, 0x56, 0x49, 0x20), //Audio Video Interleave video format
    a(0xFF, 0xFB), //MPEG-1 Layer 3 file without an I0xD3 tag or with an ID3v1 tag (which's append0xed at the end of the file),
    a(0x49, 0x44, 0x33), //MP3 file with an ID3v2 container
    a(0x42, 0x4D), //BMP file, a bitmap format us0xed mostly in the Windows world
    a(0x43, 0x44, 0x30, 0x30, 0x31), //ISO960x60 CD/DVD image file
    a(0x53, 0x49, 0x4D, 0x50, 0x4C, 0x45, 0x20, 0x20, 0x3D, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x54), //Flexible Image Transport System (FITS),
    a(0x66, 0x4C, 0x61, 0x43), //Fr0xee Lossless Audio Codec
    a(0x4D, 0x54, 0x68, 0x64), //MIDI sound file
    a(0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1), //Compound File Binary Format, a container format us0xed for document by older versions of Microsoft Office. It is however an open format us0xed by other programs as well.
    a(0x64, 0x65, 0x78, 0x0A, 0x30, 0x33, 0x35, 0x00), //Dalvik Executable
    a(0x4B, 0x44, 0x4D), //VMDK files
    a(0x43, 0x72, 0x32, 0x34), //Google Chrome extension
    a(0x41, 0x47, 0x44, 0x33), //FreeHand 8 document
    a(0x05, 0x07, 0x00, 0x00, 0x42, 0x4F, 0x42, 0x4F, 0x05, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01), //AppleWorks 5 document
    a(0x06, 0x07, 0xE1, 0x00, 0x42, 0x4F, 0x42, 0x4F, 0x06, 0x07, 0xE1, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01), //AppleWorks 6 document
    a(0x45, 0x52, 0x02, 0x00, 0x00, 0x00, 0x8B, 0x45, 0x52, 0x02, 0x00, 0x00, 0x00), //Roxio Toast disc image file, also some .dmg-files begin with same bytes
    a(0x78, 0x01, 0x73, 0x0D, 0x62, 0x62, 0x60), //Apple Disk Image file
    a(0x78, 0x61, 0x72, 0x21), //eXtensible ARchive format
    a(0x50, 0x4D, 0x4F, 0x43, 0x43, 0x4D, 0x4F, 0x43), //Windows Files And Settings Transfer Repository
    a(0x4E, 0x45, 0x53, 0x1A), //Nintendo Entertainment System ROM file
    a(0x75, 0x73, 0x74, 0x61, 0x72, 0x00, 0x30, 0x30, 0x75, 0x73, 0x74, 0x61, 0x72, 0x20, 0x20, 0x00), //tar archive
    a(0x74, 0x6F, 0x78, 0x33), //Open sour0xce portable voxel file
    a(0x4D, 0x4C, 0x56, 0x49), //Magic Lantern Video file
    a(0x44, 0x43, 0x4D, 0x01, 0x50, 0x41, 0x33, 0x30), //Windows Update Binary Delta Compression
    a(0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C), //7-Zip File Format
    a(0x1F, 0x8B), //GZIP
    a(0x04, 0x22, 0x4D, 0x18), //LZ4 Frame Format
    a(0x4D, 0x53, 0x43, 0x46), //Microsoft Cabinet file
    a(0x53, 0x5A, 0x44, 0x44, 0x88, 0xF0, 0x27, 0x33), //Microsoft compress0xed file in Quantum format, us0xed prior to Windows XP. File can 0xbe decompress0xed using Extract.exe or Expand.exe distribut0xed with earlier versions of Windows.
    a(0x46, 0x4C, 0x49, 0x46), //Fr0xee Lossless Image Format
    a(0x1A, 0x45, 0xDF, 0xA3), //Matroska media container, including WebM
    a(0x4D, 0x49, 0x4C, 0x20), //"SEAN : Session Analysis" Training file. Also us0xed in compatible software "Rpw : Rowperfect for Windows" and "RP3W : ROWPERFECT3 for Windows".
    a(0x41, 0x54, 0x26, 0x54, 0x46, 0x4F, 0x52, 0x4D, 0x00, 0x00, 0x00, 0x00, 0x44, 0x4A, 0x56), //DjVu document
    a(0x30, 0x82), //DER encod0xed X.50x09 certificate
    a(0x44, 0x49, 0x43, 0x4D), //DICOM Medical File Format
    a(0x77, 0x4F, 0x46, 0x46), //WO0xFF File Format 1.0
    a(0x77, 0x4F, 0x46, 0x32), //WO0xFF File Format 2.0
    a(0x3c, 0x3f, 0x78, 0x6d, 0x6c, 0x20), //eXtensible Markup Language when using the ASCII character encoding
    a(0x6d, 0x73, 0x61, 0x00), //WebAssembly binary format
    a(0xcf, 0x84, 0x01), //Lepton compress0xed JPEG image
    a(0x43, 0x57, 0x53, 0x46, 0x57, 0x53), //flash .swf
    a(0x21, 0x3C, 0x61, 0x72, 0x63, 0x68, 0x3E), //linux d0xeb file
    a(0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50), //Google WebP image file
    a(0x27, 0x05, 0x19, 0x56) //U-Boot / uImage. Das U-Boot Universal Boot Loader.
  )
}