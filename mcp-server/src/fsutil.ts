import fs from 'fs';
import path from 'path';

/**
 * Writes a file atomically (temp file in the same directory, then rename) so a crash
 * mid-write can never leave a half-written config behind. If the target is a symlink
 * (common with dotfile managers) the link is preserved and its real target is replaced.
 *
 * @param mode POSIX permission bits for a newly created file. When the file already
 *             exists its current permissions are kept. Ignored on Windows.
 */
export function atomicWriteFileSync(filePath: string, content: string, mode: number = 0o600): void {
  let target = filePath;
  let finalMode = mode;

  if (fs.existsSync(filePath)) {
    target = fs.realpathSync(filePath);
    finalMode = fs.statSync(target).mode & 0o777;
  }

  const dir = path.dirname(target);
  const tmp = path.join(dir, `.${path.basename(target)}.${process.pid}.${Date.now()}.tmp`);

  try {
    fs.writeFileSync(tmp, content, { encoding: 'utf8', mode: finalMode });
    fs.renameSync(tmp, target);
  } catch (err) {
    try {
      fs.unlinkSync(tmp);
    } catch {
      // temp file was never created
    }
    throw err;
  }
}
