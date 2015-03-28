package ca.mcgill.mcb.pcingola.snpSift.annotate;

import java.io.IOException;

import ca.mcgill.mcb.pcingola.fileIterator.SeekableBufferedReader;
import ca.mcgill.mcb.pcingola.fileIterator.VcfFileIterator;
import ca.mcgill.mcb.pcingola.vcf.FileIndexChrPos;
import ca.mcgill.mcb.pcingola.vcf.VcfEntry;

/**
 * Use an uncompressed sorted VCF file as a database for annotations
 *
 * Note: Assumes that the VCF database file is sorted.
 *       Each VCF entry should be sorted according to position.
 *       Chromosome order does not matter (e.g. all entries for chr10 can be before entries for chr2).
 *       But entries for the same chromosome should be together.
 *
 * Note: Old VCF specifications did not require VCF files to be sorted.
 *
 * @author pcingola
 */
public class DbVcfSorted extends DbVcf {

	protected FileIndexChrPos indexDb;

	public DbVcfSorted(String dbFileName) {
		super(dbFileName);
	}

	/**
	 * Index a VCF file
	 */
	FileIndexChrPos index(String fileName) {
		if (verbose) System.err.println("Index: " + fileName);
		FileIndexChrPos fileIndex = new FileIndexChrPos(fileName);
		fileIndex.setVerbose(verbose);
		fileIndex.setDebug(debug);
		fileIndex.open();
		fileIndex.index();
		fileIndex.close();
		return fileIndex;
	}

	/**
	 * Open database annotation file
	 */
	@Override
	public void open() {
		// Open and index database
		indexDb = index(dbFileName);

		// Re-open VCF db file
		try {
			vcfDbFile = new VcfFileIterator(new SeekableBufferedReader(dbFileName));
			vcfDbFile.setDebug(debug);
			latestVcfDb = vcfDbFile.next(); // Read first VCf entry from DB file (this also forces to read headers)
			add(latestVcfDb);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Read all DB entries up to 'vcf'
	 */
	@Override
	public void readDb(VcfEntry ve) {
		String chr = ve.getChromosomeName();

		// Do we have a DB entry from our previous iteration?
		if (latestVcfDb != null) {
			// Are we still in the same chromosome?
			if (latestVcfDb.getChromosomeName().equals(chr)) {
				updateChromo(ve);
				if (ve.getStart() < latestVcfDb.getStart()) return;
				if (ve.getStart() == latestVcfDb.getStart()) add(latestVcfDb);
			} else {
				// VcfEntry and latestDb entry are in different chromosome?
				if (checkChromo(ve)) {
					// Same chromosome.
					// This means that we finished reading all database entries from the previous chromosome.
					// There is nothing else to do until it reaches a new chromosome
					return;
				} else {
					// This means that we should jump to a database position matching VcfEntry's chromosome
					clear();

					long filePos = indexDb.getStart(chr);
					if (filePos < 0) return; // The database file does not have this chromosome
					try {
						vcfDbFile.seek(filePos);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		} else clear();

		//---
		// Read more entries from db
		//---
		for (VcfEntry vcfDb : vcfDbFile) {
			latestVcfDb = vcfDb;

			String chrDb = vcfDb.getChromosomeName();
			if (!chrDb.equals(chr)) return;

			if (ve.getStart() < vcfDb.getStart()) return;
			if (ve.getStart() == vcfDb.getStart()) {
				// Sanity check: Check that references match
				if (!ve.getRef().equals(vcfDb.getRef()) //
						&& !ve.getRef().startsWith(vcfDb.getRef()) //
						&& !vcfDb.getRef().startsWith(ve.getRef()) //
						) {
					System.err.println("WARNING: Reference in database file '" + dbFileName + "' is '" + vcfDb.getRef() + "' and reference in input file is " + ve.getRef() + "' at " + chr + ":" + (ve.getStart() + 1));
					countBadRef++;
				}

				// Same position: Add all keys to 'db'
				// Note: VCF allows more than one line with the same position
				add(vcfDb);
			}
		}
	}

}