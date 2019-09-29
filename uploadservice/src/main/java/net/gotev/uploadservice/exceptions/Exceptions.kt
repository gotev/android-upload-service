package net.gotev.uploadservice.exceptions

import net.gotev.uploadservice.network.ServerResponse

class UserCancelledUploadException : Throwable("User cancelled upload")
class UploadError(val serverResponse: ServerResponse) : Throwable("Upload error")
