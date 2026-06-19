-- CreateEnum
CREATE TYPE "TargetType" AS ENUM ('URL', 'IP');

-- AlterTable: Case — title, target type, nullable url, target IP
ALTER TABLE "Case" ADD COLUMN IF NOT EXISTS "title" TEXT NOT NULL DEFAULT 'Untitled Case';
ALTER TABLE "Case" ADD COLUMN IF NOT EXISTS "target_type" "TargetType" NOT NULL DEFAULT 'URL';
ALTER TABLE "Case" ADD COLUMN IF NOT EXISTS "target_ip" TEXT;
ALTER TABLE "Case" ALTER COLUMN "url" DROP NOT NULL;

-- AlterTable: User — remove analyst_id
DROP INDEX IF EXISTS "User_analyst_id_key";
ALTER TABLE "User" DROP COLUMN IF EXISTS "analyst_id";
