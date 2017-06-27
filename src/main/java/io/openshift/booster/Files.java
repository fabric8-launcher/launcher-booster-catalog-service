/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.booster;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class Files
{
   public static String removeFileExtension(String file)
   {
      int idx = file.lastIndexOf('.');
      return idx > 0 ? file.substring(0, idx) : file;
   }

   /**
    * @throws IOException
    */
   public static void deleteRecursively(Path path) throws IOException
   {
      if (java.nio.file.Files.isDirectory(path))
      {
         java.nio.file.Files.walkFileTree(path, new SimpleFileVisitor<Path>()
         {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
               java.nio.file.Files.delete(file);
               return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
            {
               java.nio.file.Files.delete(dir);
               return FileVisitResult.CONTINUE;
            }
         });
      }
      else
      {
         java.nio.file.Files.deleteIfExists(path);
      }
   }

}
