-- AlterTable
ALTER TABLE "Analysis" ADD COLUMN "brand_impersonated" TEXT;
ALTER TABLE "Analysis" ADD COLUMN "confidence" INTEGER;
ALTER TABLE "Analysis" ADD COLUMN "mitre_techniques" JSONB;
ALTER TABLE "Analysis" ADD COLUMN "verdict" TEXT;
